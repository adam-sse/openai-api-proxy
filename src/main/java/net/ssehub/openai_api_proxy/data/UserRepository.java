package net.ssehub.openai_api_proxy.data;

import org.springframework.data.repository.CrudRepository;

import jakarta.transaction.Transactional;

public interface UserRepository extends CrudRepository<User, String> {

    @Transactional
    public default User findOrCreate(String name) {
        return findById(name).orElseGet(() -> save(new User(name)));
    }
    
}
