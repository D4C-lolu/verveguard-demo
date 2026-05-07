package com.interswitch.verveguarddemo.services;


import com.interswitch.verveguarddemo.base.BaseIntegrationTest;
import com.interswitch.verveguarddemo.entities.User;
import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.ConflictException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.models.enums.UserStatus;
import com.interswitch.verveguarddemo.models.request.ChangePasswordRequest;
import com.interswitch.verveguarddemo.models.request.CreateUserRequest;
import com.interswitch.verveguarddemo.models.response.UserResponse;
import com.interswitch.verveguarddemo.repositories.UserRepository;
import com.interswitch.verveguarddemo.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Service Integration Tests")
public class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User superAdmin = userRepository.findByEmail("superadmin@verveguard.com").orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(superAdmin), null, List.of())
        );
    }

    private CreateUserRequest buildCreateRequest(String email, String phone, Long roleId) {
        return new CreateUserRequest("John", "Doe", null, email, phone, "Admin123!", roleId);
    }

    // -------------------------------------------------------------------------
    // createUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should create user successfully")
    void shouldCreateUserSuccessfully() {
        UserResponse response = userService.createUser(buildCreateRequest("john.doe@test.com", "55555555555", 2L));

        assertThat(response.id()).isNotNull().isPositive();
        assertThat(response.firstname()).isEqualTo("John");
        assertThat(response.lastname()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john.doe@test.com");
        assertThat(response.phone()).isEqualTo("55555555555");
        assertThat(response.userStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("should populate createdBy when creating user")
    void shouldPopulateCreatedByWhenCreatingUser() {
        User superAdmin = userRepository.findByEmail("superadmin@verveguard.com").orElseThrow();

        UserResponse response = userService.createUser(buildCreateRequest("jane.doe@test.com", "66666666668", 2L));
        User saved = userRepository.findById(response.id()).orElseThrow();

        assertThat(saved.getCreatedBy()).isEqualTo(superAdmin.getId());
    }

    @Test
    @DisplayName("should fail create user with duplicate email")
    void shouldFailCreateUserWithDuplicateEmail() {
        assertThatThrownBy(() -> userService.createUser(buildCreateRequest("testadmin@verveguard.com", "77777777777", 2L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("should fail create user with duplicate phone")
    void shouldFailCreateUserWithDuplicatePhone() {
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        assertThatThrownBy(() -> userService.createUser(buildCreateRequest("unique@test.com", existing.getPhone(), 2L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("should fail create user with invalid role")
    void shouldFailCreateUserWithInvalidRole() {
        assertThatThrownBy(() -> userService.createUser(buildCreateRequest("unique2@test.com", "99999999999", 9999L)))
                .isInstanceOf(BadRequestException.class); // caught by validateForCreate
    }

    // -------------------------------------------------------------------------
    // getUserById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get user by id successfully")
    void shouldGetUserByIdSuccessfully() {
        User existing = userRepository.findByEmail("superadmin@verveguard.com").orElseThrow();

        UserResponse response = userService.getUserById(existing.getId());

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.email()).isEqualTo(existing.getEmail());
    }

    @Test
    @DisplayName("should fail get user with non existent id")
    void shouldFailGetUserWithNonExistentId() {
        assertThatThrownBy(() -> userService.getUserById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    // -------------------------------------------------------------------------
    // getCurrentUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get current user successfully")
    void shouldGetCurrentUserSuccessfully() {
        User superAdmin = userRepository.findByEmail("superadmin@verveguard.com").orElseThrow();

        UserResponse response = userService.getCurrentUser();

        assertThat(response.id()).isEqualTo(superAdmin.getId());
        assertThat(response.email()).isEqualTo("superadmin@verveguard.com");
    }

    // -------------------------------------------------------------------------
    // getAllUsers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get all users paginated")
    void shouldGetAllUsersPaginated() {
        Page<UserResponse> page = userService.getAllUsers(1, 10, "createdAt", Sort.Direction.DESC);

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().size()).isLessThanOrEqualTo(10);
    }

    // -------------------------------------------------------------------------
    // changeUserStatus
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should change user status successfully")
    void shouldChangeUserStatusSuccessfully() {
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        assertThatNoException().isThrownBy(() ->
                userService.changeUserStatus(existing.getId(), UserStatus.SUSPENDED)
        );

        forceFlush();

        User updated = userRepository.findById(existing.getId()).orElseThrow();
        assertThat(updated.getUserStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    // -------------------------------------------------------------------------
    // changeUserRole
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should change user role successfully")
    void shouldChangeUserRoleSuccessfully() {
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();
        // role ID 2 = ADMIN, which is PrincipalType.ADMIN — valid for users
        assertThatNoException().isThrownBy(() ->
                userService.changeUserRole(existing.getId(), 2L)
        );
    }

    @Test
    @DisplayName("should fail change user role with non-admin role")
    void shouldFailChangeUserRoleWithNonAdminRole() {
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();
        // role ID 3 = MERCHANT, PrincipalType.MERCHANT — invalid for users
        assertThatThrownBy(() -> userService.changeUserRole(existing.getId(), 3L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid role for user");
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should change password successfully")
    void shouldChangePasswordSuccessfully() {
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        assertThatNoException().isThrownBy(() ->
                userService.changePassword(existing.getId(),
                        new ChangePasswordRequest("Admin123!", "NewPassword123!", "NewPassword123!"))
        );
    }

    @Test
    @DisplayName("should fail change password with wrong current password")
    void shouldFailChangePasswordWithWrongCurrentPassword() {
        User existing = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        assertThatThrownBy(() ->
                userService.changePassword(existing.getId(),
                        new ChangePasswordRequest("WrongPassword!", "NewPassword123!", "NewPassword123!"))
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid credentials");
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should soft delete user successfully")
    void shouldSoftDeleteUserSuccessfully() {
        User superAdmin = userRepository.findByEmail("superadmin@verveguard.com").orElseThrow();
        User toDelete = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();

        userService.deleteUser(toDelete.getId());

        forceFlush();
        User deleted = userRepository.findById(toDelete.getId()).orElseThrow();
        assertThat(deleted.isNotDeleted()).isFalse();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getDeletedBy()).isEqualTo(superAdmin.getId());
    }

    @Test
    @DisplayName("should not find soft deleted user")
    void shouldNotFindSoftDeletedUser() {
        User toDelete = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow();
        userService.deleteUser(toDelete.getId());

        assertThatThrownBy(() -> userService.getUserById(toDelete.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }
}