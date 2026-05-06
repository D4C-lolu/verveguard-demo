package com.interswitch.verveguarddemo.mapper;

import com.interswitch.verveguarddemo.entities.Permission;
import com.interswitch.verveguarddemo.models.response.PermissionResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse map(Permission permission);

    List<PermissionResponse> map(List<Permission> permissions);
}