package net.ssehub.openai_api_proxy.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;

import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import net.ssehub.openai_api_proxy.controllers.exceptions.InvalidRequestException;
import net.ssehub.openai_api_proxy.controllers.exceptions.NoUserSpecifiedException;
import net.ssehub.openai_api_proxy.data.Model;
import net.ssehub.openai_api_proxy.data.ModelRepository;
import net.ssehub.openai_api_proxy.data.Query;
import net.ssehub.openai_api_proxy.data.QueryRepository;
import net.ssehub.openai_api_proxy.data.Ratelimit;
import net.ssehub.openai_api_proxy.data.RatelimitRepository;
import net.ssehub.openai_api_proxy.data.User;
import net.ssehub.openai_api_proxy.data.UserRepository;
import net.ssehub.openai_api_proxy.dto.openai_api.ChatCompletionRequest;
import net.ssehub.openai_api_proxy.dto.openai_api.ChatCompletionResponse;
import net.ssehub.openai_api_proxy.dto.openai_api.Message;
import net.ssehub.openai_api_proxy.dto.openai_api.Usage;

@RestController
public class ProxyController {

    private static final Logger LOG = Logger.getLogger(ProxyController.class.getName());
    
    private UserRepository userRepository;
    
    private QueryRepository queryRepository;
    
    private ModelRepository modelRepository;
    
    private RatelimitRepository ratelimitRepository;
    
    private EncodingRegistry encodingRegistry;
    
    @Value("${forward.url}")
    private String forwardUrl;
    
    @Value("${forward.token}")
    private String forwardToken;
    
    public ProxyController(UserRepository userRepository, QueryRepository queryRepository,
            ModelRepository modelRepository, RatelimitRepository ratelimitRepository,
            EncodingRegistry encodingRegistry) {
        this.userRepository = userRepository;
        this.queryRepository = queryRepository;
        this.modelRepository = modelRepository;
        this.ratelimitRepository = ratelimitRepository;
        this.encodingRegistry = encodingRegistry;
    }
    
    public void setForwardUrl(String forwardBaseUrl) {
        this.forwardUrl = forwardBaseUrl;
    }
    
    public void setForwardToken(String forwardToken) {
        this.forwardToken = forwardToken;
    }
    
    @PostMapping("/v1/chat/completions")
    public ResponseEntity<?> request(RequestEntity<ChatCompletionRequest> request)
            throws NoUserSpecifiedException, InvalidRequestException {

        User user = checkUser(request);
        LOG.info(() -> "Request from " + user.getName() + ": " + request.getBody());
        checkRequest(request.getBody());
        estimateAndLogTokens(request.getBody());
        
        LOG.info(() -> "Forwarding to " + forwardUrl);
        RequestBodySpec forwardedRequest = RestClient.create()
                .post()
                .uri(forwardUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forwardToken)
                .body(request.getBody());
        
        ErrorHandler errorHandler = new ErrorHandler();
        ResponseEntity<ChatCompletionResponse> forwardResponse = forwardedRequest
                .retrieve()
                .onStatus(errorHandler)
                .toEntity(ChatCompletionResponse.class);
        
        ResponseEntity<?> response;
        if (!errorHandler.caughtError()) {
            analyzeResponse(user, forwardResponse);
            response = constructResponse(forwardResponse);
        } else {
            response = errorHandler.getErrorResponse();
        }
        return response;
    }

    private void analyzeResponse(User user, ResponseEntity<ChatCompletionResponse> response) {
        ChatCompletionResponse responseBody = response.getBody();
        
        LOG.info(() -> "Forward response status: " + response.getStatusCode());
    
        Usage usage = responseBody.usage();
        Model model = modelRepository.findOrCreate(responseBody.model());
        
        Query query = new Query(user, model, usage.promptTokens(), usage.completionTokens());
        
        LOG.info(() -> "User " + query.getUser() + " used " + query.getQueryTokens() + " query and "
                + query.getAnswerTokens() + " answer tokens with model " + query.getModel()
                + " (cost: " + query.calculateCost() + " $)");
        queryRepository.save(query);
        
        List<Ratelimit> ratelimits = new LinkedList<>();
        HttpHeaders headers = response.getHeaders();
        ratelimits.add(parseRatelimit(headers, "requests"));
        ratelimits.add(parseRatelimit(headers, "tokens"));
        ratelimitRepository.setCurrentRatelimits(ratelimits);
    }
    
    private Ratelimit parseRatelimit(HttpHeaders headers, String name) {
        String limit = headers.getFirst("x-ratelimit-limit-" + name);
        String remaining = headers.getFirst("x-ratelimit-remaining-" + name);
        String reset = headers.getFirst("x-ratelimit-reset-" + name);
        
        Ratelimit ratelimit = new Ratelimit(name, limit, remaining, reset);
        
        LOG.info(() -> ratelimit.toString());
        
        return ratelimit;
    }
    
    private void estimateAndLogTokens(ChatCompletionRequest request) {
        Encoding encoding = encodingRegistry.getEncodingForModel(request.model()).orElseGet(() -> {
            LOG.warning(() -> "Could not get encoding for requested model " + request.model()
                    + ", falling back to cl100k_base");
            return encodingRegistry.getEncoding(EncodingType.CL100K_BASE);
        });
        
        // https://jtokkit.knuddels.de/docs/getting-started/recipes/chatml
        int tokensPerMessage = 0;
        int tokensPerName = 0;
        if (request.model().startsWith("gpt-4")) {
            tokensPerMessage = 3;
            tokensPerName = 1;
        } else if (request.model().startsWith("gpt-3.5-turbo")) {
            tokensPerMessage = 4;
            tokensPerName = -1;
        }
        
        try {
            int tokens = 3;
            for (Message message : request.messages()) {
                tokens += tokensPerMessage;
                tokens += encoding.countTokens(message.content());
                tokens += encoding.countTokens(message.role().name().toLowerCase());
                if (message.name() != null) {
                    tokens += encoding.countTokens(message.name());
                    tokens += tokensPerName;
                }
            }
            
            int t = tokens;
            LOG.info(() -> "Estimated tokens for query: " + t);
            
        } catch (UnsupportedOperationException e) {
            LOG.log(Level.WARNING, "Failed to count tokens in query", e);
        }
    }

    private User checkUser(RequestEntity<ChatCompletionRequest> request) throws NoUserSpecifiedException {
        HttpHeaders headers = request.getHeaders();
        if (headers.getFirst("x-user") == null) {
            LOG.info(() -> "Request has no x-user header set");
            throw new NoUserSpecifiedException();
        }
        User user = userRepository.findOrCreate(headers.getFirst("x-user"));
        return user;
    }
    
    private static void checkRequest(ChatCompletionRequest request) throws InvalidRequestException {
        if (request.messages() == null) {
            throw new InvalidRequestException();
        }
        if (request.messages().isEmpty()) {
            throw new InvalidRequestException();
        }
        for (Message message : request.messages()) {
            checkMessage(message);
        }
        if (request.model() == null) {
            throw new InvalidRequestException();
        }
    }
    
    private static void checkMessage(Message message) throws InvalidRequestException {
        if (message == null) {
            throw new InvalidRequestException();
        }
        if (message.content() == null) {
            throw new InvalidRequestException();
        }
        if (message.role() == null) {
            throw new InvalidRequestException();
        }
    }

    private static HttpHeaders copyHeaders(HttpHeaders headers) {
        HttpHeaders copiedHeaders = new HttpHeaders();
        copiedHeaders.addAll(headers);
        copiedHeaders.remove(":status");
        copiedHeaders.clearContentHeaders();
        return copiedHeaders;
    }

    private static ResponseEntity<?> constructResponse(ResponseEntity<ChatCompletionResponse> forwardResponse) {
        ResponseEntity<?> response;
        HttpHeaders copiedHeaders = copyHeaders(forwardResponse.getHeaders());
        response = ResponseEntity.status(forwardResponse.getStatusCode())
                .headers(copiedHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(forwardResponse.getBody());
        return response;
    }

    private static class ErrorHandler implements ResponseErrorHandler {

        private ResponseEntity<?> errorResponse;

        public boolean caughtError() {
            return errorResponse != null;
        }
        
        public ResponseEntity<?> getErrorResponse() {
            return errorResponse;
        }
        
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return response.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            HttpStatusCode statusCode = response.getStatusCode();
            String statusText =  response.getStatusText();
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            
            LOG.warning(() -> "Got HTTP error from forwarded request: " + statusCode.value() + " " + statusText);
            LOG.info(() -> "Error body:\n" + body);
            
            HttpHeaders copiedHeaders = copyHeaders(response.getHeaders());
            this.errorResponse = ResponseEntity.status(response.getStatusCode())
                    .headers(copiedHeaders)
                    .contentType(response.getHeaders().getContentType())
                    .body(body);
        }

    }
    
}
