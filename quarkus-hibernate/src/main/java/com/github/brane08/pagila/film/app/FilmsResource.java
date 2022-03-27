package com.github.brane08.pagila.film.app;

import com.github.brane08.pagila.film.beans.FilmInfo;
import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.film.mapper.FilmMapper;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.seedworks.app.FiqlQueryBean;
import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/films")
@Produces(MediaType.APPLICATION_JSON)
public class FilmsResource {

    private final FilmsRepository repository;
    private final FilmMapper mapper;

    @Inject
    public FilmsResource(FilmsRepository repository, FilmMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GET
    public Response list(@QueryParam("qry") @DefaultValue("") String qry,
                         @QueryParam("page") @DefaultValue("1") int page,
                         @QueryParam("size") @DefaultValue("20") int size,
                         @QueryParam("sort") @DefaultValue("id") String sort,
                         @QueryParam("direction") @DefaultValue("1") int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build(qry, page, size, sort, direction);
        PagedList<FilmInfo> list = repository.page(fiqlBean.qry, fiqlBean.pageInfo(), mapper::filmToInfo);
        return Response.ok(ApiResult.array(list)).build();
    }

    @GET
    @Path("/@view")
    public Response listViews(@QueryParam("qry") @DefaultValue("") String qry,
                              @QueryParam("page") @DefaultValue("1") int page,
                              @QueryParam("size") @DefaultValue("20") int size,
                              @QueryParam("sort") @DefaultValue("id") String sort,
                              @QueryParam("direction") @DefaultValue("1") int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build(qry, page, size, sort, direction);
        PagedList<FilmViewInfo> list = repository.listFilms(fiqlBean.pageInfo());
        return Response.ok(ApiResult.array(list.list(), list.totalCount())).build();
    }
}
