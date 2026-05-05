package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.entities.Permission;
import com.interswitch.verveguarddemo.exceptions.ConflictException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.mapper.PermissionMapper;
import com.interswitch.verveguarddemo.models.request.CreatePermissionRequest;
import com.interswitch.verveguarddemo.models.response.PermissionResponse;
import com.interswitch.verveguarddemo.repositories.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.name())) {
            throw new ConflictException("Permission already exists");
        }

        Permission permission = Permission.builder()
                .name(request.name())
                .description(request.description())
                .build();

        return permissionMapper.map(permissionRepository.save(permission));
    }

    public PermissionResponse getPermissionById(Long permissionId) {
        return permissionMapper.map(permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException("Permission not found")));
    }

    public List<PermissionResponse> getAllPermissions() {
        return permissionMapper.map(permissionRepository.findAll());
    }
}