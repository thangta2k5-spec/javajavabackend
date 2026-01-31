package com.tathang.example304.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tathang.example304.model.ERole;
import com.tathang.example304.model.Role;
import com.tathang.example304.repository.RoleRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByName(ERole.ROLE_ADMIN).isEmpty()) {
                roleRepository.save(new Role(ERole.ROLE_ADMIN));
            }
            if (roleRepository.findByName(ERole.ROLE_MANAGER).isEmpty()) {
                roleRepository.save(new Role(ERole.ROLE_MANAGER));
            }
            if (roleRepository.findByName(ERole.ROLE_STAFF).isEmpty()) {
                roleRepository.save(new Role(ERole.ROLE_STAFF));
            }
            if (roleRepository.findByName(ERole.ROLE_CUSTOMER).isEmpty()) {
                roleRepository.save(new Role(ERole.ROLE_CUSTOMER));
            }
        };
    }
}
