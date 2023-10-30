package com.github.brane08.pagila.pages.film;

import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.seedworks.app.MapBasedSortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class FilmsDataProvider implements ISortableDataProvider<FilmViewInfo, String> {

    private static final Logger LOG = LoggerFactory.getLogger(FilmsDataProvider.class);

    private final transient FilmsRepository repository;
    private final MapBasedSortState sortState;

    public FilmsDataProvider(FilmsRepository repository) {
        this.repository = repository;
        this.sortState = new MapBasedSortState();
    }


    @Override
    public Iterator<FilmViewInfo> iterator(long first, long count) {
        List<String> sortFields = sortState.propertySet().stream().map(expr -> {
            SortOrder sortOrder = sortState.getPropertySortOrder(expr);
            if (sortOrder == SortOrder.DESCENDING) {
                return "-" + expr;
            }
            return expr;
        }).toList();
        String order = sortFields.isEmpty() ? "filmId" : String.join(",", sortFields);
        return repository.offsetOfFilms((int) first, (int) count, order).list().iterator();
    }

    @Override
    public long size() {
        return repository.count("");
    }

    @Override
    public IModel<FilmViewInfo> model(FilmViewInfo object) {
        return new Model<>(object);
    }

    @Override
    public ISortState<String> getSortState() {
        return sortState;
    }


}
