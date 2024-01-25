package net.ssehub.openai_api_proxy.dto;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import net.ssehub.openai_api_proxy.data.Ratelimit;

public class RatelimitsDto {
    
    private Map<String, Ratelimit> ratelimits = new HashMap<>();
    
    @JsonAnySetter
    public void add(String name, Ratelimit ratelimit) {
        this.ratelimits.put(name, ratelimit);
    }
    
    public Ratelimit get(String name) {
        return this.ratelimits.get(name);
    }
    
    @JsonAnyGetter
    public Map<String, Ratelimit> getRatelimits() {
        return Collections.unmodifiableMap(ratelimits);
    }
    
    public static void main(String[] args) {
        System.out.println(Duration.parse("PT0.120s").getNano());
    }

}
