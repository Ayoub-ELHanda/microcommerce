package com.ecommerce.commandmicrocommerce.repository;

import com.ecommerce.commandmicrocommerce.model.Command;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandRepository extends MongoRepository<Command, String> {
    
    List<Command> findByClientId(String clientId);
    List<Command> findByStatus(String status);
    List<Command> findByClientEmail(String clientEmail);
} 