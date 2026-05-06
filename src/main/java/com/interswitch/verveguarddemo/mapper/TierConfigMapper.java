package com.interswitch.verveguarddemo.mapper;

import com.interswitch.verveguarddemo.entities.TierConfig;
import com.interswitch.verveguarddemo.models.response.TierConfigResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TierConfigMapper {
    TierConfigResponse map(TierConfig tierConfig);

    List<TierConfigResponse> map(List<TierConfig> tierConfigs);
}