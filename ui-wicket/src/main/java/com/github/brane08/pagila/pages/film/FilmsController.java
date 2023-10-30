package com.github.brane08.pagila.pages.film;

import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.seedworks.app.FiqlQueryBean;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;

@Named
@ApplicationScoped
public class FilmsController {

    @Inject
    FilmsRepository repository;

    public PagedList<FilmViewInfo> listViews(String qry, int page, int size) {
        return listViews(qry, page, size, "id", 1);
    }

    public PagedList<FilmViewInfo> listViews(String qry, int page, int size, String sort, int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build(qry, page, size, sort, direction);
        return repository.listFilms(fiqlBean.pageInfo());
    }

    public ISortableDataProvider<FilmViewInfo, String> viewInfoProvider() {
        return new FilmsDataProvider(repository);
    }
}
