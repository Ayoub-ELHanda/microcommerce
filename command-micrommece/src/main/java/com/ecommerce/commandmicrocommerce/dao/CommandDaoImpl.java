package com.ecommerce.commandmicrocommerce.dao;

import com.ecommerce.commandmicrocommerce.model.Command;
import com.ecommerce.commandmicrocommerce.model.CommandItem;
import com.ecommerce.commandmicrocommerce.repository.CommandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Repository
public class CommandDaoImpl implements CommandDao {
    
    @Autowired
    private CommandRepository commandRepository;
    
    @PostConstruct
    public void initData() {
        if (commandRepository.count() == 0) {
            // Commande 1
            Command command1 = new Command();
            command1.setClientId("client1");
            command1.setClientName("Jean Dupont");
            command1.setClientEmail("jean.dupont@email.com");
            command1.setShippingAddress("123 Rue de la Paix, Paris 75001");
            command1.setStatus("CONFIRMED");
            command1.setPaymentMethod("CARTE_CREDIT");
            command1.setItems(Arrays.asList(
                new CommandItem("prod1", "Ordinateur portable", 1, 800.0),
                new CommandItem("prod2", "Souris sans fil", 2, 25.0)
            ));
            command1.calculateTotalAmount();
            
            // Commande 2
            Command command2 = new Command();
            command2.setClientId("client2");
            command2.setClientName("Marie Martin");
            command2.setClientEmail("marie.martin@email.com");
            command2.setShippingAddress("456 Avenue des Champs, Lyon 69001");
            command2.setStatus("SHIPPED");
            command2.setPaymentMethod("PAYPAL");
            command2.setDeliveryDate(LocalDateTime.now().plusDays(2));
            command2.setItems(Arrays.asList(
                new CommandItem("prod3", "Clavier mécanique", 1, 120.0),
                new CommandItem("prod4", "Écran 24 pouces", 1, 250.0)
            ));
            command2.calculateTotalAmount();
            
            // Commande 3
            Command command3 = new Command();
            command3.setClientId("client3");
            command3.setClientName("Pierre Bernard");
            command3.setClientEmail("pierre.bernard@email.com");
            command3.setShippingAddress("789 Boulevard Voltaire, Marseille 13001");
            command3.setStatus("PENDING");
            command3.setPaymentMethod("VIREMENT");
            command3.setItems(Arrays.asList(
                new CommandItem("prod5", "Casque audio", 1, 80.0),
                new CommandItem("prod6", "Webcam HD", 1, 60.0),
                new CommandItem("prod7", "Tapis de souris", 3, 15.0)
            ));
            command3.calculateTotalAmount();
            
            commandRepository.saveAll(Arrays.asList(command1, command2, command3));
        }
    }
    
    @Override
    public List<Command> findAll() {
        return commandRepository.findAll();
    }
    
    @Override
    public Command findById(String id) {
        return commandRepository.findById(id).orElse(null);
    }
    
    @Override
    public Command save(Command command) {
        command.calculateTotalAmount(); // Recalculer le total avant sauvegarde
        return commandRepository.save(command);
    }
    
    @Override
    public void deleteById(String id) {
        commandRepository.deleteById(id);
    }
    
    @Override
    public List<Command> findByClientId(String clientId) {
        return commandRepository.findByClientId(clientId);
    }
    
    @Override
    public List<Command> findByStatus(String status) {
        return commandRepository.findByStatus(status);
    }
} 