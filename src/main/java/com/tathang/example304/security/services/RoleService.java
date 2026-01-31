package com.tathang.example304.security.services;

import org.springframework.stereotype.Service;

import com.tathang.example304.model.Role;
import com.tathang.example304.repository.RoleRepository;

import java.util.List;

@Service
public class RoleService {
     
    private final RoleRepository roleRepository;
    
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}