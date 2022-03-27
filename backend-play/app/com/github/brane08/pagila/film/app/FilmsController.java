package com.github.brane08.pagila.film.app;

import com.github.brane08.pagila.film.infra.FilmsMapper;
import com.github.brane08.pagila.film.repositories.FilmsRepository;
import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PageRequest;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class FilmsController extends Controller {

	private final MessagesApi messagesApi;
	private final HttpExecutionContext context;
	private final FilmsRepository repository;
	private final FilmsMapper mapper;

	@Inject
	public FilmsController(HttpExecutionContext context, MessagesApi messagesApi,
						   FilmsRepository repository, FilmsMapper mapper) {
		this.messagesApi = messagesApi;
		this.context = context;
		this.repository = repository;
		this.mapper = mapper;
	}

	public CompletionStage<Result> list(final int page, final int size, final String order, final String filter) {
		return repository.page(PageRequest.from(page, size, order, filter), mapper::map).thenApplyAsync(list -> {
			// This is the HTTP rendering thread context
			return ok(Json.toJson(ApiResult.array(list.getList(), list.getTotalCount())));
		}, context.current());
	}
}
