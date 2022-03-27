package com.github.brane08.pagila.actor.app;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.infra.ActorsMapper;
import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PageRequest;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import com.github.brane08.pagila.actor.repositories.ActorsRepository;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class ActorsController extends Controller {

	private final MessagesApi messagesApi;
	private final HttpExecutionContext executionContext;
	private final ActorsRepository repository;
	private final ActorsMapper mapper;

	@Inject
	public ActorsController(HttpExecutionContext httpExecutionContext, MessagesApi messagesApi,
							ActorsRepository repository, ActorsMapper mapper) {
		this.messagesApi = messagesApi;
		this.executionContext = httpExecutionContext;
		this.repository = repository;
		this.mapper = mapper;
	}

	public CompletionStage<Result> list(int page, int size, String order, String filter) {
		// Run a db operation in another thread (using DatabaseExecutionContext)
		return repository.page(PageRequest.from(page, size, order, filter),mapper::map).thenApplyAsync(list -> {
			// This is the HTTP rendering thread context
			return ok(Json.toJson(ApiResult.array(list.getList(), list.getTotalCount())));
		}, executionContext.current());
	}
}
