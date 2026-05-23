package com.bidly.authservice.repository;

import com.bidly.authservice.entity.Role;
import com.bidly.authservice.enums.Rolename;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(Rolename name);
}
