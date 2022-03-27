package com.github.brane08.pagila.actor.mapper;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.beans.ActorViewInfo;
import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.actor.entities.ActorView;
import com.github.brane08.pagila.seedworks.mapper.CommonConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CommonConfig.class)
public interface ActorMapper {

    ActorInfo actorToInfo(Actor actor);

    List<ActorInfo> actorsToInfos(List<Actor> actors);

    ActorViewInfo actorViewToInfo(ActorView view);

    List<ActorViewInfo> actorViewsToInfo(List<ActorView> view);
}
