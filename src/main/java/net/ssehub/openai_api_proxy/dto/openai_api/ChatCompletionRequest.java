package net.ssehub.openai_api_proxy.dto.openai_api;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public record ChatCompletionRequest(
        List<Message> messages,
        String model,
        @JsonProperty("frequency_penalty") Double frequencyPenalty,
        @JsonProperty("logit_bias") Map<Long, Number> logitBias,
        Boolean logprobs,
        @JsonProperty("top_logprobs") Integer topLogprobs,
        @JsonProperty("max_tokens") Integer maxTokens,
        Integer n,
        @JsonProperty("presence_penalty") Double presencePenalty,
        @JsonProperty("response_format") ResponseFormat responseFormat,
        Integer seed,
        Object stop,
        Boolean stream,
        Double temperature,
        @JsonProperty("top_p") Double topP,
        String user) {

    private static void appendIfNotNull(StringBuilder builder, Object value, String name) {
        if (value != null) {
            builder.append(", ");
            builder.append(name);
            builder.append('=');
            builder.append(value);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ChatGptRequest [");
        builder.append("messages=");
        builder.append(messages);
        builder.append(", ");
        builder.append("model=");
        builder.append(model);
        appendIfNotNull(builder, frequencyPenalty, "frequency_penalty");
        appendIfNotNull(builder, logitBias, "logit_bias");
        appendIfNotNull(builder, logprobs, "logprobs");
        appendIfNotNull(builder, topLogprobs, "top_logprobs");
        appendIfNotNull(builder, maxTokens, "max_tokens");
        appendIfNotNull(builder, n, "n");
        appendIfNotNull(builder, presencePenalty, "presence_penalty");
        appendIfNotNull(builder, responseFormat, "response_format");
        appendIfNotNull(builder, seed, "seed");
        appendIfNotNull(builder, stop, "stop");
        appendIfNotNull(builder, stream, "stream");
        appendIfNotNull(builder, temperature, "temperature");
        appendIfNotNull(builder, topP, "top_p");
        appendIfNotNull(builder, user, "user");
        builder.append("]");
        return builder.toString();
    }
    
}
