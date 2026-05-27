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

import java.util.Optional;

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
    public Response page(@QueryParam("qry") @DefaultValue("") String qry,
                         @QueryParam("page") @DefaultValue("1") int page,
                         @QueryParam("size") @DefaultValue("20") int size,
                         @QueryParam("sort") @DefaultValue("storeId") String sort,
                         @QueryParam("direction") @DefaultValue("1") int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build(qry, page, size, sort, direction);
        PagedList<StoreInfo> list = repository.page(fiqlBean.qry, fiqlBean.pageInfo(), mapper::storeToInfo);
        return Response.ok(ApiResult.array(list)).build();
    }

    @GET
    @Path("/@sales-by-store")
    public Response salesByStore() {
        return Response.ok(ApiResult.array(repository.allSalesByStore())).build();
    }

    @GET
    @Path("/@staff")
    public Response listStaff() {
        return Response.ok(ApiResult.array(repository.listStaffViews())).build();
    }

    @GET
    @Path("/@view")
    public Response listView() {
        return Response.ok(ApiResult.array(repository.listStoreViews())).build();
    }

    @GET
    @Path("/{storeId: \\d+}")
    public Response getById(@PathParam("storeId") int storeId) {
        Optional<StoreInfo> found = repository.findById(storeId, mapper::storeToInfo);
        return found.map(s -> Response.ok(ApiResult.single(s)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{storeId: \\d+}/inventory")
    public Response storeInventory(@PathParam("storeId") int storeId) {
        return Response.ok(ApiResult.array(repository.getStoreInventory(storeId))).build();
    }

    @GET
    @Path("/{storeId: \\d+}/rentals")
    public Response storeRentals(@PathParam("storeId") int storeId) {
        return Response.ok(ApiResult.array(repository.getStoreRentals(storeId))).build();
    }

    @GET
    @Path("/{storeId: \\d+}/customers")
    public Response storeCustomers(@PathParam("storeId") int storeId) {
        return Response.ok(ApiResult.array(repository.getStoreTopCustomers(storeId))).build();
    }
}
