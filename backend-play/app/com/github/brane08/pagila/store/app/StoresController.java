package com.github.brane08.pagila.store.app;

import com.github.brane08.pagila.seedworks.beans.ApiResult;
import com.github.brane08.pagila.seedworks.beans.PageRequest;
import com.github.brane08.pagila.store.infra.StoresMapper;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class StoresController extends Controller {
	private final MessagesApi messagesApi;
	private final HttpExecutionContext executionContext;
	private final StoreRepository repository;
	private final StoresMapper mapper;

	@Inject
	public StoresController(HttpExecutionContext httpExecutionContext, MessagesApi messagesApi,
							StoreRepository repository, StoresMapper mapper) {
		this.messagesApi = messagesApi;
		this.executionContext = httpExecutionContext;
		this.repository = repository;
		this.mapper = mapper;
	}

	public CompletionStage<Result> list(int page, int size, String order, String filter) {
		// Run a db operation in another thread (using DatabaseExecutionContext)
		return repository.page(PageRequest.from(page, size, order, filter), mapper::map).thenApplyAsync(list -> {
			// This is the HTTP rendering thread context
			return ok(Json.toJson(ApiResult.array(list.getList(), list.getTotalCount())));
		}, executionContext.current());
	}
}
