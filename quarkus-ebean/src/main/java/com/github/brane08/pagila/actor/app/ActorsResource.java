package com.github.brane08.pagila.actor.app;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import com.github.brane08.pagila.seedworks.app.FiqlQueryBean;
import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/actors")
@Produces(MediaType.APPLICATION_JSON)
public class ActorsResource {

    private final ActorsRepository repository;
    private final ActorMapper mapper;

    @Inject
    public ActorsResource(ActorsRepository repository, ActorMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GET
    public Response list(@QueryParam("qry") @DefaultValue("") String qry,
                         @QueryParam("page") @DefaultValue("1") int page,
                         @QueryParam("size") @DefaultValue("20") int size,
                         @QueryParam("sort") @DefaultValue("actorId") String sort,
                         @QueryParam("direction") @DefaultValue("1") int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build(qry, page, size, sort, direction);
        PagedList<ActorInfo> list = repository.page(fiqlBean.qry, fiqlBean.pageInfo(), mapper::actorToInfo);
        return Response.ok(ApiResult.array(list)).build();
    }
}
