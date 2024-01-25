package net.ssehub.openai_api_proxy.data;

import org.springframework.data.repository.CrudRepository;

import jakarta.transaction.Transactional;

public interface RatelimitRepository extends CrudRepository<Ratelimit, String> {

    @Transactional
    public default void setCurrentRatelimits(Iterable<Ratelimit> ratelimits) {
        deleteAll();
        saveAll(ratelimits);
    }
    
}
