package net.ssehub.openai_api_proxy.dto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.ssehub.openai_api_proxy.data.Cost;
import net.ssehub.openai_api_proxy.data.Model;

public class ModelDto {

    private String name;
    
    private List<CostDto> costs;
    
    public ModelDto(Model model) {
        this.name = model.getName();
        
        this.costs = new ArrayList<>();
        for (Cost cost : model.getAllCosts()) {
            this.costs.add(new CostDto(cost.getValidFrom(), cost.getPer1KQueryTokens(), cost.getPer1KAnswerTokens()));
        }
        this.costs.sort(Comparator.comparing(CostDto::validFrom));
    }
    
    public String getName() {
        return name;
    }
    
    public List<CostDto> getCosts() {
        return costs;
    }
    
}
