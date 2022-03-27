package com.github.brane08.pagila.seedworks.infra;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.WARN,
		componentModel = "jsr330")
public interface CommonConfig {
}
