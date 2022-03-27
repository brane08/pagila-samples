package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.PageRequest;
import com.github.brane08.pagila.seedworks.entities.PagedList;
import com.github.brane08.pagila.seedworks.infra.DatabaseExecutionContext;
import com.github.brane08.pagila.seedworks.infra.query.QueryParser;
import play.db.jpa.JPAApi;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public abstract class Repository<T> {

	protected final JPAApi ja;
	protected final DatabaseExecutionContext context;
	protected final Class<T> clazz;
	protected final String filterProperty;

	public Repository(JPAApi ja, DatabaseExecutionContext context, Class<T> clazz,
					  String filterProperty) {
		this.ja = ja;
		this.context = context;
		this.clazz = clazz;
		this.filterProperty = filterProperty;
	}

	public <R> CompletionStage<PagedList<R>> page(final PageRequest request, Function<T, R> mapper) {
		return supplyAsync(() -> wrap(em -> {
			QueryParser<T> parser = new QueryParser<>(em, clazz, request.filter);
			TypedQuery<T> typedQuery = parser.buildQuery();
			List<T> list = typedQuery.setFirstResult(request.offset()).setMaxResults(request.size).getResultList();
			long count = parser.buildCountQuery().getResultList().stream().findAny().orElseThrow();
			;
			return new PagedList<R>(list.stream().map(mapper).collect(Collectors.toList()),
					(int) count, 0, false, request.page);
		}), context);
	}

	protected List<String> getLoadFields() {
		return Collections.emptyList();
	}

	protected <I> I wrap(Function<EntityManager, I> function) {
		return ja.withTransaction(function);
	}

}
