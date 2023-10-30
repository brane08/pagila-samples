package com.github.brane08.pagila.pages.actor;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import com.github.brane08.pagila.seedworks.app.MapBasedSortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Iterator;

public class ActorsDataProvider implements ISortableDataProvider<ActorInfo, String> {

    private final transient ActorsRepository repository;
    private final transient ActorMapper mapper;
    private final MapBasedSortState sortState;

    public ActorsDataProvider(ActorsRepository repository, ActorMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.sortState = new MapBasedSortState();
    }


    @Override
    public Iterator<ActorInfo> iterator(long first, long count) {
        return repository.pageOffset("", (int) first, (int) count, "id", mapper::actorToInfo).list().iterator();
    }

    @Override
    public long size() {
        return repository.count("");
    }

    @Override
    public IModel<ActorInfo> model(ActorInfo object) {
        return new Model<>(object);
    }

    @Override
    public ISortState<String> getSortState() {
        return sortState;
    }
}
