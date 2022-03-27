package com.github.brane08.pagila.store.app;

import com.github.brane08.pagila.seedworks.app.FiqlQueryBean;
import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.store.beans.StoreInfo;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/stores")
@Produces(MediaType.APPLICATION_JSON)
public class StoresResource {
    private final StoreRepository repository;
    private final StoreMapper mapper;

    @Inject
    public StoresResource(StoreRepository repository, StoreMapper mapper) {
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
        PagedList<StoreInfo> list = repository.page(fiqlBean.qry, fiqlBean.pageInfo(), mapper::storeToInfo);
        return Response.ok(ApiResult.array(list)).build();
    }
}
