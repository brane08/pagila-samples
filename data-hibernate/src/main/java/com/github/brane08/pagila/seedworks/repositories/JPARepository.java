package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.JPAQueryParser;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.query.QueryParserFactory;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class JPARepository<T, ID> implements Repository<T, ID>, MappingRepository<T, ID>, QueryParserFactory {

    private final EntityManager em;

    protected JPARepository(EntityManager em) {
        this.em = em;
    }


    protected EntityManager em() {
        return em;
    }

    protected List<String> getLoadFields() {
        return Collections.emptyList();
    }

    protected abstract Class<T> entityClass();

    protected abstract String filterProperty();

    @Override
    public <R> QueryParser<R> buildParser(String queryString, Class<R> resultClass) {
        return new JPAQueryParser<>(em, queryString, resultClass);
    }

    @Override
    @Transactional
    public int count(String queryPart) {
        QueryParser<T> parser = buildParser(queryPart, entityClass());
        return parser.getCount();
    }

    @Override
    @Transactional
    public PagedList<T> page(String queryPart, final PageInfo request) {
        QueryParser<T> parser = buildParser(queryPart, entityClass());
        return new PagedList<>(parser.getResults(request), parser.getCount());
    }

    @Override
    @Transactional
    public <R> PagedList<R> page(String queryPart, final PageInfo request, Function<T, R> mapper) {
        PagedList<T> list = page(queryPart, request);
        return new PagedList<>(list.list().stream().map(mapper).collect(Collectors.toList()), list.totalCount());
    }

    @Override
    public <R> Optional<R> findById(ID id, Function<T, R> mapper) {
        Optional<T> byIdOpt = findById(id);
        return byIdOpt.map(mapper);
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(em.find(entityClass(), id));
    }
}
