package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.JPAQueryParser;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.query.QueryParserFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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
    public PagedList<T> pageOffset(String queryPart, int offset, int size, String order) {
        QueryParser<T> parser = buildParser(queryPart, entityClass());
        return new PagedList<>(parser.getResults(offset, size, order), parser.getCount());
    }

    @Override
    @Transactional
    public <R> PagedList<R> page(String queryPart, final PageInfo request, Function<T, R> mapper) {
        PagedList<T> list = page(queryPart, request);
        return new PagedList<>(list.list().stream().map(mapper).collect(Collectors.toList()), list.totalCount());
    }

    @Override
    public <R> PagedList<R> pageOffset(String queryPart, int offset, int size, String order, Function<T, R> mapper) {
        PagedList<T> list = pageOffset(queryPart, offset, size, order);
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

    @Override
    public <R> List<R> all(Function<T, R> mapper) {
        return all().stream().map(mapper).collect(Collectors.toList());
    }

    @Override
    public List<T> all() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cbQuery = cb.createQuery(entityClass());
        Root<T> root = cbQuery.from(entityClass());
        cbQuery.select(root);
        TypedQuery<T> query = em.createQuery(cbQuery);
        return query.getResultList();
    }
}
