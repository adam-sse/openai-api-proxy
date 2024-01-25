package net.ssehub.openai_api_proxy.controllers;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.ssehub.openai_api_proxy.controllers.exceptions.NoSuchModelException;
import net.ssehub.openai_api_proxy.controllers.exceptions.NoSuchUserException;
import net.ssehub.openai_api_proxy.data.Cost;
import net.ssehub.openai_api_proxy.data.CostRepository;
import net.ssehub.openai_api_proxy.data.Model;
import net.ssehub.openai_api_proxy.data.ModelRepository;
import net.ssehub.openai_api_proxy.data.Query;
import net.ssehub.openai_api_proxy.data.QueryRepository;
import net.ssehub.openai_api_proxy.data.Ratelimit;
import net.ssehub.openai_api_proxy.data.RatelimitRepository;
import net.ssehub.openai_api_proxy.data.User;
import net.ssehub.openai_api_proxy.data.UserRepository;
import net.ssehub.openai_api_proxy.dto.CostDto;
import net.ssehub.openai_api_proxy.dto.ModelDto;
import net.ssehub.openai_api_proxy.dto.QueryDto;
import net.ssehub.openai_api_proxy.dto.RatelimitsDto;
import net.ssehub.openai_api_proxy.dto.UsageDto;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class UsageController {

    private UserRepository userRepository;
    
    private QueryRepository queryRepository;
    
    private ModelRepository modelRepository;
    
    private CostRepository costRepository;
    
    private RatelimitRepository ratelimitRepository;
    
    public UsageController(UserRepository userRepository, QueryRepository queryRepository,
            ModelRepository modelRepository, CostRepository costRepository, RatelimitRepository ratelimitRepository) {
        this.userRepository = userRepository;
        this.queryRepository = queryRepository;
        this.modelRepository = modelRepository;
        this.costRepository = costRepository;
        this.ratelimitRepository = ratelimitRepository;
    }
    
    @GetMapping("/usage")
    public UsageDto getUsage(@RequestParam(name = "user", required = false) String username,
            @RequestParam(required = false) ZonedDateTime after,
            @RequestParam(required = false) ZonedDateTime before)
            throws NoSuchUserException {
        
        List<QueryDto> queries = getQueries(username, after, before);
        
        UsageDto usage = new UsageDto();
        double cost = 0.0;
        for (QueryDto query : queries) {
            usage.add(query);
            cost += query.cost();
        }
        usage.setCost(cost);
        
        return usage;
    }
    
    @GetMapping("/queries")
    public List<QueryDto> getQueries(@RequestParam(name = "user", required = false) String username,
            @RequestParam(required = false) ZonedDateTime after,
            @RequestParam(required = false) ZonedDateTime before)
            throws NoSuchUserException {
        Iterable<Query> dbResult;
        if (username != null) {
            User user = userRepository.findById(username).orElseThrow(() -> new NoSuchUserException());
            dbResult = queryRepository.findByUser(user);
        } else {
            dbResult = queryRepository.findAll();
        }
        
        Stream<Query> filtered = StreamSupport.stream(dbResult.spliterator(), false);
        if (after != null) {
            filtered = filtered.filter(query -> query.getTimestamp().isAfter(after));
        }
        if (before != null) {
            filtered = filtered.filter(query -> query.getTimestamp().isBefore(before));
        }
        
        return filtered
                .sorted(Comparator.comparing(query -> query.getTimestamp()))
                .map(QueryDto::new)
                .toList();
    }
    
    @GetMapping("/models")
    public List<ModelDto> getModels() {
        return StreamSupport.stream(modelRepository.findAll().spliterator(), false).map(ModelDto::new).toList();
    }
    
    @GetMapping("/model/{name}")
    public ModelDto getModel(@PathVariable(name = "name") String modelName) throws NoSuchModelException {
        return new ModelDto(modelRepository.findById(modelName).orElseThrow(() -> new NoSuchModelException()));
    }
    
    @PostMapping(path = "/model/{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void getModel(@RequestBody CostDto dto, @PathVariable(name = "name") String modelName) {
        Model model = modelRepository.findOrCreate(modelName);
        
        Cost cost = new Cost();
        cost.setValidFrom(dto.validFrom());
        cost.setPer1KQueryTokens(dto.per1KQueryTokens());
        cost.setPer1KAnswerTokens(dto.per1KAnswerTokens());
        cost = model.addOrUpdateCost(cost);
        
        costRepository.save(cost);
        modelRepository.save(model);
    }
    
    @GetMapping("/ratelimits")
    public RatelimitsDto getRatelimits() {
        RatelimitsDto dto = new RatelimitsDto();
        for (Ratelimit ratelimit : ratelimitRepository.findAll()) {
            dto.add(ratelimit.getType(), ratelimit);
        }
        return dto;
    }
    
}
