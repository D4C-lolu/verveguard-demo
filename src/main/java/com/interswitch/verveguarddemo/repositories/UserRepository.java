package com.interswitch.verveguarddemo.repositories;

import com.interswitch.verveguarddemo.entities.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Cacheable(value = "user-by-email", key = "#email")
    @Query("SELECT u FROM User u JOIN FETCH u.role r LEFT JOIN FETCH r.permissions WHERE u.email = :email")
    Optional<User> findByEmail(String email);

    @Caching(evict = {
            @CacheEvict(value = "user-by-email", allEntries = true),
            @CacheEvict(value = "user-role-name", allEntries = true)
    })
    @Query(value = "SELECT sp_user_update_role(:id, :roleId)", nativeQuery = true)
    void updateRole(@Param("id") Long id, @Param("roleId") Long roleId);
}
