package com.github.brane08.pagila.actor.app;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.mapper.ActorMapper;
import com.github.brane08.pagila.actor.repositories.ActorsRepository;
import com.github.brane08.pagila.seedworks.app.FiqlQueryBean;
import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/actors")
public class ActorsResource {

    @CheckedTemplate
    public static class Templates {

        public static native TemplateInstance list(List<ActorInfo> todos);
        public static native TemplateInstance item(ActorInfo todo);
    }

    private final ActorsRepository repository;
    private final ActorMapper mapper;

    @Inject
    public ActorsResource(ActorsRepository repository, ActorMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GET
    public TemplateInstance list(@QueryParam("qry") @DefaultValue("") String qry,
                                 @QueryParam("page") @DefaultValue("1") int page,
                                 @QueryParam("size") @DefaultValue("20") int size,
                                 @QueryParam("sort") @DefaultValue("actorId") String sort,
                                 @QueryParam("direction") @DefaultValue("1") int direction) {
        FiqlQueryBean fiqlBean = FiqlQueryBean.build(qry, page, size, sort, direction);
        return showList(repository.page(fiqlBean.qry, fiqlBean.pageInfo(), mapper::actorToInfo));
    }

    private TemplateInstance showList(PagedList<ActorInfo> paged) {
        return Templates.list(paged.list()).data("total", paged.totalCount());
    }
}
