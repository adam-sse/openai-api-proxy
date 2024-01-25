package net.ssehub.openai_api_proxy.dto.openai_api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record ChatCompletionResponse(
        String id,
        List<Choice> choices,
        Integer created,
        String model,
        @JsonProperty("system_fingerprint") String systemFingerprint,
        String object,
        Usage usage) {

}
