package com.github.brane08.pagila.seedworks.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.ReportingPolicy;

@MapperConfig(unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.IGNORE,
        suppressTimestampInGenerated = true, injectionStrategy = InjectionStrategy.CONSTRUCTOR, nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface CommonConfig {
}
