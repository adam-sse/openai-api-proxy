package net.ssehub.openai_api_proxy.data;

import org.springframework.data.repository.CrudRepository;

public interface QueryRepository extends CrudRepository<Query, Long> {
    
    public Iterable<Query> findByUser(User user);
    
}
