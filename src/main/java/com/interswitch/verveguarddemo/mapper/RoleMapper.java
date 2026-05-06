package com.interswitch.verveguarddemo.mapper;

import com.interswitch.verveguarddemo.entities.Role;
import com.interswitch.verveguarddemo.models.response.RoleResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse map(Role role);

    List<RoleResponse> map(List<Role> roles);
}

