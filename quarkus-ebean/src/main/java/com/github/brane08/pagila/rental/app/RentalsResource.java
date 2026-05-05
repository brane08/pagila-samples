package com.github.brane08.pagila.rental.app;

import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.beans.RentalInfo;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.rental.repositories.RentalRepository;
import com.github.brane08.pagila.seedworks.app.FiqlQueryBean;
import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/rentals")
@Produces(MediaType.APPLICATION_JSON)
public class RentalsResource {

    private final RentalRepository repository;
    private final RentalMapper mapper;

    @Inject
    public RentalsResource(RentalRepository repository, RentalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GET
    public Response page(@QueryParam("qry") @DefaultValue("") String qry,
                         @QueryParam("page") @DefaultValue("1") int page,
                         @QueryParam("size") @DefaultValue("20") int size,
                         @QueryParam("sort") @DefaultValue("id") String sort,
                         @QueryParam("direction") @DefaultValue("1") int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build(qry, page, size, sort, direction);
        PagedList<RentalInfo> list = repository.page(fiqlBean.qry, fiqlBean.pageInfo(), mapper::rentalToInfo);
        return Response.ok(ApiResult.array(list)).build();
    }

    @GET
    @Path("/@customers")
    public Response listCustomers(@QueryParam("page") @DefaultValue("1") int page,
                                  @QueryParam("size") @DefaultValue("20") int size,
                                  @QueryParam("sort") @DefaultValue("id") String sort,
                                  @QueryParam("direction") @DefaultValue("1") int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build("", page, size, sort, direction);
        PagedList<CustomerViewInfo> list = repository.listCustomerViews(
                fiqlBean.pageInfo().offset(), fiqlBean.pageInfo().size(), fiqlBean.pageInfo().order());
        return Response.ok(ApiResult.array(list.list(), list.totalCount())).build();
    }
}
