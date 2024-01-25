package net.ssehub.openai_api_proxy.dto.openai_api;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Role {
    @JsonProperty("system") SYSTEM,
    @JsonProperty("user") USER,
    @JsonProperty("assistant") ASSISTANT,
}
