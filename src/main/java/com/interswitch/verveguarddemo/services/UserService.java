package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.entities.Role;
import com.interswitch.verveguarddemo.entities.User;
import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import com.interswitch.verveguarddemo.models.enums.UserStatus;
import com.interswitch.verveguarddemo.models.request.ChangePasswordRequest;
import com.interswitch.verveguarddemo.models.request.CreateUserRequest;
import com.interswitch.verveguarddemo.models.response.UserResponse;
import com.interswitch.verveguarddemo.repositories.RoleRepository;
import com.interswitch.verveguarddemo.repositories.UserRepository;
import com.interswitch.verveguarddemo.util.SecurityUtil;
import com.interswitch.verveguarddemo.util.ValidationUtil;
import com.interswitch.verveguarddemo.constants.CacheId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.USERS, key = "#result.id"),
            @CacheEvict(value = CacheId.Names.USERS_PAGE, allEntries = true)
    })
    public UserResponse createUser(CreateUserRequest request) {
        List<Map<String, Object>> conflicts = userRepository.validateForCreate(request.email(), request.phone(), request.roleId());
        ValidationUtil.checkConflicts(conflicts);

        Long currentUserId = SecurityUtil.findCurrentUserId().orElse(null);

        User user = User.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .othername(request.othername())
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(new Role(request.roleId()))
                .userStatus(UserStatus.ACTIVE)
                .build();

        user.setCreatedBy(currentUserId);
        userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .othername(user.getOthername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleName(user.getRole().getName())
                .userStatus(user.getUserStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Cacheable(value = CacheId.Names.USERS, key = "#id")
    public UserResponse getUserById(Long id) {
        return userRepository.findUserById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Cacheable(value = CacheId.Names.USERS, key = "T(com.interswitch.verveguarddemo.util.SecurityUtil).getCurrentUserId()")
    public UserResponse getCurrentUser() {
        return getUserById(SecurityUtil.getCurrentUserId());
    }

    @Cacheable(value = CacheId.Names.USERS_PAGE, key = "#page + '-' + #size + '-' + #sortField + '-' + #direction")
    public Page<UserResponse> getAllUsers(int page, int size, String sortField, Sort.Direction direction) {
        return userRepository.findAllUsers(PageRequest.of(page - 1, size, Sort.by(direction, sortField)));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.USERS, key = "#id"),
            @CacheEvict(value = CacheId.Names.USERS_PAGE, allEntries = true)
    })
    public void changeUserStatus(Long id, UserStatus status) {
        userRepository.updateStatus(id, status, SecurityUtil.getCurrentUserId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.USERS, key = "#id"),
            @CacheEvict(value = CacheId.Names.USERS_PAGE, allEntries = true)
    })
    public void changeUserRole(Long id, Long roleId) {
        if (!roleRepository.existsByIdAndPrincipalType(roleId, PrincipalType.ADMIN)) {
            throw new BadRequestException("Invalid role for user");
        }
        userRepository.updateRole(id, roleId, SecurityUtil.getCurrentUserId());
    }

    @Transactional
    @CacheEvict(value = CacheId.Names.USERS, key = "#id")
    public void changePassword(Long id, ChangePasswordRequest request) {
        String currentHash = userRepository.findPasswordHashById(id);
        if (currentHash == null || !passwordEncoder.matches(request.currentPassword(), currentHash)) {
            throw new BadRequestException("Invalid credentials");
        }
        userRepository.updatePassword(id, passwordEncoder.encode(request.newPassword()), SecurityUtil.getCurrentUserId());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheId.Names.USERS, key = "#id"),
            @CacheEvict(value = CacheId.Names.USERS_PAGE, allEntries = true)
    })
    public void deleteUser(Long id) {
        userRepository.softDelete(id, SecurityUtil.getCurrentUserId());
    }
}