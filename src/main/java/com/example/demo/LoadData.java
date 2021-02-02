package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class LoadData {
    private CustomerRepository customerRepository;

    @Autowired
    public LoadData(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Bean
    public CommandLineRunner initDB(CustomerRepository customerRepository) {
        return  args -> {
            log.info("pre loading : " + customerRepository.save(new Customer("pas", "active")));
            log.info("pre loading : " + customerRepository.save(new Customer("lucia", "active")));
            log.info("pre loading : " + customerRepository.save(new Customer("lucas", "inactive")));
            log.info("pre loading : " + customerRepository.save(new Customer("siena", "inactive")));
            log.info("pre loading : " + customerRepository.save(new Customer("jeff", "inactive")));
            log.info("pre loading : " + customerRepository.save(new Customer("daniel", "inactive")));
            log.info("pre loading : " + customerRepository.save(new Customer("emily", "active")));

        };
    }
}
