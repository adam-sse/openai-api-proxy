package net.ssehub.openai_api_proxy.dto.openai_api;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FinishReason {
    @JsonProperty("stop") STOP,
    @JsonProperty("length") LENGTH,
    @JsonProperty("content_filter") CONTENT_FILTER,
}
