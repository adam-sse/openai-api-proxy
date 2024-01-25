package net.ssehub.openai_api_proxy.data;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Model {

    @Id
    private String name;
    
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "model")
    private Set<Cost> costs = new HashSet<>();
    
    public Model(String name) {
        this.name = name;
    }

    // for JPA
    protected Model() {
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Cost getCostAt(ZonedDateTime timestamp) {
        Cost latestCostBeforeTimestamp = null;
        for (Cost cost : costs) {
            if (cost.getValidFrom().isBefore(timestamp)) {
                if (latestCostBeforeTimestamp == null
                        || cost.getValidFrom().isAfter(latestCostBeforeTimestamp.getValidFrom())) {
                    latestCostBeforeTimestamp = cost;
                }
            }
        }
        return latestCostBeforeTimestamp;
    }
    
    public Cost addOrUpdateCost(Cost newCost) {
        Cost existing = null;
        for (Cost other : this.costs) {
            if (other.getValidFrom().equals(newCost.getValidFrom())) {
                existing = other;
                break;
            }
        }
        
        Cost result;
        if (existing == null) {
            newCost.setModel(this);
            this.costs.add(newCost);
            result = newCost;
        } else {
            existing.setPer1KQueryTokens(newCost.getPer1KQueryTokens());
            existing.setPer1KAnswerTokens(newCost.getPer1KAnswerTokens());
            result = existing;
        }
        return result;
    }
    
    public Set<Cost> getAllCosts() {
        return Collections.unmodifiableSet(costs);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Model)) {
            return false;
        }
        Model other = (Model) obj;
        return Objects.equals(name, other.name);
    }
    
}
