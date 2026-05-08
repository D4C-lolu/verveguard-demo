package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        PrincipalType principalType,
        List<PermissionResponse> permissions
) {
}
