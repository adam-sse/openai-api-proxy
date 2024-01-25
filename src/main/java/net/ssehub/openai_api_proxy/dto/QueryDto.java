package net.ssehub.openai_api_proxy.dto;

import java.time.ZonedDateTime;

import net.ssehub.openai_api_proxy.data.Query;

public record QueryDto(
        String user,
        String model,
        ZonedDateTime timestamp,
        long queryTokens,
        long answerTokens,
        double cost) {

    public QueryDto(Query query) {
        this(
                query.getUser().getName(),
                query.getModel().getName(),
                query.getTimestamp(),
                query.getQueryTokens(),
                query.getAnswerTokens(),
                query.calculateCost());
    }
    
}
