package com.interswitch.verveguarddemo.repositories;

import com.interswitch.verveguarddemo.entities.User;
import com.interswitch.verveguarddemo.models.enums.UserStatus;
import com.interswitch.verveguarddemo.models.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u JOIN FETCH u.role r LEFT JOIN FETCH r.permissions WHERE u.email = :email")
    Optional<User> findByEmail(String email);

    @Query("SELECT new com.interswitch.verveguarddemo.models.response.UserResponse(u.id, u.firstname, u.lastname, u.othername, u.email, u.phone, u.role.name, u.userStatus, u.createdAt, u.updatedAt) FROM User u WHERE u.id = :id")
    Optional<UserResponse> findUserById(Long id);


    @Modifying
    @Query("UPDATE User u SET u.firstname = :firstname, u.lastname = :lastname, u.othername = :othername, u.phone = :phone, u.email = :email, u.updatedBy = :updatedBy WHERE u.id = :id")
    void updateUser(Long id, String firstname, String lastname, String othername, String phone, String email, Long updatedBy);

    @Modifying
    @Query("""
                UPDATE User u
                SET u.updatedBy = :deletedBy,
                    u.deletedAt = CURRENT_TIMESTAMP
                WHERE u.id = :userId
            """)
    void softDelete(Long userId, Long deletedBy);

    @Query("""
            SELECT
                u.id          AS id,
                (u.email = :email) AS email_exists,
                (u.phone = :phone) AS phone_exists,
                (r.principalType <> 'ADMIN') AS invalid_role
            FROM User u
            JOIN u.role r
            WHERE u.email = :email OR u.phone = :phone
            OR r.id = :roleId
            """)
    List<Map<String, Object>> validateForCreate(String email, String phone, Long roleId);

    @Query("SELECT u.passwordHash FROM User u WHERE u.id = :id")
    String findPasswordHashById(Long id);

    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash, u.updatedBy = :updatedBy WHERE u.id = :id")
    void updatePassword(Long id, String passwordHash, Long updatedBy);

    @Modifying
    @Query("UPDATE User u SET u.userStatus = :userStatus, u.updatedBy = :updatedBy WHERE u.id = :id")
    void updateStatus(Long id, UserStatus userStatus, Long updatedBy);

    @Modifying
    @Query("UPDATE User u SET u.role.id = :roleId, u.updatedBy = :updatedBy WHERE u.id = :id")
    void updateRole(Long id, Long roleId, Long updatedBy);

    @Query("""
                SELECT new  com.interswitch.verveguarddemo.models.response.UserResponse(
                    u.id, 
                    u.firstname, 
                    u.lastname, 
                    u.othername, 
                    u.email, 
                    u.phone, 
                    u.role.name, 
                    u.userStatus, 
                    u.createdAt, 
                    u.updatedAt
                ) 
                FROM User u
            """)
    Page<UserResponse> findAllUsers(Pageable pageable);
}