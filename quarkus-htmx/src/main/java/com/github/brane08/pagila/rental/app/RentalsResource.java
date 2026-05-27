package com.github.brane08.pagila.rental.app;

import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.rental.repositories.RentalRepository;
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

@Path("/rentals")
public class RentalsResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance customerList(List<CustomerViewInfo> customers);
    }

    private final RentalRepository repository;
    private final RentalMapper mapper;

    @Inject
    public RentalsResource(RentalRepository repository, RentalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GET
    @Path("/@customers")
    public TemplateInstance customerList(@QueryParam("page") @DefaultValue("1") int page,
                                         @QueryParam("size") @DefaultValue("20") int size,
                                         @QueryParam("sort") @DefaultValue("id") String sort) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build("", page, size, sort, 1);
        PagedList<CustomerViewInfo> paged = repository.listCustomerViews(
                fiqlBean.pageInfo().offset(), fiqlBean.pageInfo().size(), fiqlBean.pageInfo().order());
        return Templates.customerList(paged.list())
                .data("total", paged.totalCount())
                .data("page", page)
                .data("size", size)
                .data("hasPrev", page > 1)
                .data("hasNext", (long) page * size < paged.totalCount())
                .data("prevPage", page - 1)
                .data("nextPage", page + 1);
    }
}
