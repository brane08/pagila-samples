package com.github.brane08.pagila.film.boundary;

import com.github.brane08.pagila.film.beans.FilmInfo;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.seedworks.app.FiqlQueryBean;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.List;

@Path("/ui/films")
public class FilmsResource {

    @CheckedTemplate(requireTypeSafeExpressions = false)
    public static class Templates {

        public static native TemplateInstance list(List<FilmInfo> films);
    }

    private final FilmsRepository repository;
    private final FilmMapper mapper;

    @Inject
    public FilmsResource(FilmsRepository repository, FilmMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GET
    public TemplateInstance list(@QueryParam("qry") @DefaultValue("") String qry,
                                 @QueryParam("page") @DefaultValue("1") int page,
                                 @QueryParam("size") @DefaultValue("20") int size,
                                 @QueryParam("sort") @DefaultValue("filmId") String sort,
                                 @QueryParam("direction") @DefaultValue("1") int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build(qry, page, size, sort, direction);
        PagedList<FilmInfo> paged = repository.page(fiqlBean.qry, fiqlBean.pageInfo(), mapper::filmToInfo);
        return Templates.list(paged.list())
                .data("total", paged.totalCount())
                .data("page", page)
                .data("size", size)
                .data("hasPrev", page > 1)
                .data("hasNext", (long) page * size < paged.totalCount())
                .data("prevPage", page - 1)
                .data("nextPage", page + 1);
    }
}
