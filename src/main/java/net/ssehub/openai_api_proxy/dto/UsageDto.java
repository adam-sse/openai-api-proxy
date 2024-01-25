package net.ssehub.openai_api_proxy.dto;

public class UsageDto {

    private int numQueries;
    
    private long queryTokens;
    
    private long answerTokens;
    
    private double cost;
    
    public void add(QueryDto query) {
        this.numQueries++;
        this.queryTokens += query.queryTokens();
        this.answerTokens += query.answerTokens();
    }
    
    public int getNumQueries() {
        return numQueries;
    }
    
    public long getQueryTokens() {
        return queryTokens;
    }
    
    public long getAnswerTokens() {
        return answerTokens;
    }
    
    public double getCost() {
        return cost;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
    }
    
    public double getAverageCostPerQuery() {
        return cost / numQueries;
    }
    
}
