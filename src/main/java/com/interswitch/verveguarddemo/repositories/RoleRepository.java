package com.interswitch.verveguarddemo.repositories;


import com.interswitch.verveguarddemo.entities.Role;
import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByName(String name);

    Optional<Role> findByName(String name);

    boolean existsByIdAndPrincipalType(Long id, PrincipalType principalType);
}


