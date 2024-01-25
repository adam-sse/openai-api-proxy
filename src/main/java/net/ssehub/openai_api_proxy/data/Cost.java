package net.ssehub.openai_api_proxy.data;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"model_name", "valid_from"}))
public class Cost {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    
    @ManyToOne(optional = false)
    private Model model;
    
    @Column(nullable = false)
    private ZonedDateTime validFrom;
    
    private double per1KQueryTokens;
    
    private double per1KAnswerTokens;
    
    void setModel(Model model) {
        this.model = model;
    }
    
    public Model getModel() {
        return model;
    }
    
    public void setValidFrom(ZonedDateTime validFrom) {
        this.validFrom = validFrom;
    }
    
    public ZonedDateTime getValidFrom() {
        return validFrom;
    }

    public void setPer1KQueryTokens(double per1KQueryTokens) {
        this.per1KQueryTokens = per1KQueryTokens;
    }
    
    public double getPer1KQueryTokens() {
        return per1KQueryTokens;
    }
    
    public void setPer1KAnswerTokens(double per1KAnswerTokens) {
        this.per1KAnswerTokens = per1KAnswerTokens;
    }
    
    public double getPer1KAnswerTokens() {
        return per1KAnswerTokens;
    }
    
}
