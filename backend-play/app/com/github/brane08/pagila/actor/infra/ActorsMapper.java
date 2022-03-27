package com.github.brane08.pagila.actor.infra;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.seedworks.infra.CommonConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(config = CommonConfig.class)
public interface ActorsMapper {
	ActorsMapper INSTANCE = Mappers.getMapper(ActorsMapper.class);

	ActorInfo map(Actor actor);

	List<ActorInfo> map(List<Actor> actors);
}
