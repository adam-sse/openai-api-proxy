package net.ssehub.openai_api_proxy.data;

import org.springframework.data.repository.CrudRepository;

import jakarta.transaction.Transactional;

public interface ModelRepository extends CrudRepository<Model, String> {
    
    @Transactional
    public default Model findOrCreate(String name) {
        return findById(name).orElseGet(() -> save(new Model(name)));
    }
    
}
