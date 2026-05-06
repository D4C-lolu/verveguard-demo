package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.annotation.ValidSortField;
import com.interswitch.verveguarddemo.constants.Permissions;
import com.interswitch.verveguarddemo.entities.User;
import com.interswitch.verveguarddemo.models.enums.UserStatus;
import com.interswitch.verveguarddemo.models.request.ChangePasswordRequest;
import com.interswitch.verveguarddemo.models.request.CreateUserRequest;
import com.interswitch.verveguarddemo.models.response.UserResponse;
import com.interswitch.verveguarddemo.services.UserService;
import com.interswitch.verveguarddemo.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Management", description = "Endpoints for managing system users, status updates, role assignments, and password changes")
@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Create User",
            description = "Register a new admin user in the system. Role must have ADMIN principal type. Requires USER_CREATE authority."
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.USER_CREATE + "')")
    public UserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.createUser(request);
    }

    @Operation(
            summary = "Get Current User",
            description = "Returns the profile of the currently authenticated admin user derived from their JWT token."
    )
    @GetMapping("me")
    @PreAuthorize("hasAuthority('" + Permissions.USER_READ + "')")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @Operation(
            summary = "List All Users",
            description = "Retrieve a paginated list of all system users with configurable sorting. Requires USER_READ authority."
    )
    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.USER_READ + "')")
    public Page<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @ValidSortField(target = User.class) @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return userService.getAllUsers(page, size, sortField, sortDirection);
    }

    @Operation(
            summary = "Get User by ID",
            description = "Fetch profile information for a specific user by their ID. Requires USER_READ authority."
    )
    @GetMapping("{userId}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_READ + "')")
    public UserResponse getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @Operation(
            summary = "Change User Status",
            description = "Update the account status of a specific user (e.g. ACTIVE, INACTIVE). Requires USER_UPDATE authority."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{userId}/status")
    @PreAuthorize("hasAuthority('" + Permissions.USER_UPDATE + "')")
    public void changeUserStatus(
            @PathVariable Long userId,
            @RequestParam UserStatus status
    ) {
        userService.changeUserStatus(userId, status);
    }

    @Operation(
            summary = "Change User Role",
            description = "Assign a new role to a specific user. Role must have ADMIN principal type. Requires USER_UPDATE authority."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{userId}/role")
    @PreAuthorize("hasAuthority('" + Permissions.USER_UPDATE + "')")
    public void changeUserRole(
            @PathVariable Long userId,
            @RequestParam Long roleId
    ) {
        userService.changeUserRole(userId, roleId);
    }

    @Operation(
            summary = "Change My Password",
            description = "Allows the currently authenticated user to update their own password by verifying their current password first."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("me/password")
    @PreAuthorize("hasAuthority('" + Permissions.USER_READ + "')")
    public void changeMyPassword(@RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(SecurityUtil.getCurrentUserId(), request);
    }

    @Operation(
            summary = "Change User Password (Admin)",
            description = "Allows an admin to reset a specific user's password. Requires USER_UPDATE authority."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("{userId}/password")
    @PreAuthorize("hasAuthority('" + Permissions.USER_UPDATE + "')")
    public void changePassword(
            @PathVariable Long userId,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        userService.changePassword(userId, request);
    }

    @Operation(
            summary = "Delete User",
            description = "Soft delete a user account from the system. Requires USER_DELETE authority."
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("{userId}")
    @PreAuthorize("hasAuthority('" + Permissions.USER_DELETE + "')")
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }
}