package com.github.brane08.pagila.actor.repositories;

import com.github.brane08.pagila.actor.beans.ActorViewInfo;
import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.actor.entities.ActorView;
import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import io.ebean.Database;
import org.mapstruct.factory.Mappers;

import java.util.List;

public class ActorsRepository extends EbeanRepository<Actor, Integer> {

    private final ActorMapper mapper;

    public ActorsRepository(Database db) {
        super(db);
        this.mapper = Mappers.getMapper(ActorMapper.class);
    }

    @Override
    protected Class<Actor> entityClass() {
        return Actor.class;
    }

    @Override
    protected String filterProperty() {
        return "firstName";
    }

    public PagedList<ActorViewInfo> listActorViews(int offset, int size, String order) {
        QueryParser<ActorView> parser = buildParser("", ActorView.class);
        List<ActorView> list = parser.getResults(offset, size, order);
        long count = parser.getCount();
        return new PagedList<>(mapper.actorViewsToInfo(list), (int) count);
    }
}
