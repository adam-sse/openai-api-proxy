package net.ssehub.openai_api_proxy.data;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Query {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @ManyToOne(optional = false)
    private User user;
    
    @ManyToOne(optional = false)
    private Model model;
    
    private long queryTokens;
    
    private long answerTokens;
    
    @Column(nullable = false)
    private ZonedDateTime timestamp;
    
    public Query(User user, Model model, long queryTokens, long answerTokens) {
        this.user = user;
        this.model = model;
        this.queryTokens = queryTokens;
        this.answerTokens = answerTokens;
        this.timestamp = ZonedDateTime.now();
    }
    
    // for JPA
    protected Query() {
    }
    
    public User getUser() {
        return user;
    }
    
    public Model getModel() {
        return model;
    }
    
    public long getQueryTokens() {
        return queryTokens;
    }
    
    public long getAnswerTokens() {
        return answerTokens;
    }
    
    public long totalTokens() {
        return queryTokens + answerTokens;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public double calculateCost() {
        Cost cost = model.getCostAt(timestamp);
        double result = 0.0;
        if (cost != null) {
            result = (queryTokens * cost.getPer1KQueryTokens() + answerTokens * cost.getPer1KAnswerTokens()) / 1000.0;
        }
        return result;
    }
    
}
