package net.ssehub.openai_api_proxy.dto.openai_api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.ALWAYS)
public record Choice(
        @JsonProperty("finish_reason") FinishReason finishReason,
        int index,
        Message message,
        Object logprobs) {

}
