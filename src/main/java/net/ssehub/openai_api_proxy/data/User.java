package net.ssehub.openai_api_proxy.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class User {
    
    @Id
    private String name;
    
    public User(String name) {
        this.name = name;
    }
    
    // for JPA
    protected User() {
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
