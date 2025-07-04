package com.ecommerce.commandmicrocommerce.dao;

import com.ecommerce.commandmicrocommerce.model.Command;

import java.util.List;

public interface CommandDao {
    
    List<Command> findAll();
    Command findById(String id);
    Command save(Command command);
    void deleteById(String id);
    
    List<Command> findByClientId(String clientId);
    List<Command> findByStatus(String status);
} 