package net.ssehub.openai_api_proxy.dto;

import java.time.ZonedDateTime;

public record CostDto(ZonedDateTime validFrom, double per1KQueryTokens, double per1KAnswerTokens) {

}
