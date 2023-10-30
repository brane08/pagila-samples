package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.EbeanQueryParser;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.query.QueryParserFactory;
import io.ebean.Database;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class EbeanRepository<T, ID> implements Repository<T, ID>, MappingRepository<T, ID>, QueryParserFactory {

    private final Database db;

    protected EbeanRepository(Database db) {
        this.db = db;
    }

    protected Database db() {
        return db;
    }

    protected List<String> getLoadFields() {
        return Collections.emptyList();
    }

    protected abstract Class<T> entityClass();

    protected abstract String filterProperty();

    @Override
    public <R> QueryParser<R> buildParser(String queryString, Class<R> resultClass) {
        return new EbeanQueryParser<>(db, queryString, resultClass);
    }

    @Override
    public <R> Optional<R> findById(ID id, Function<T, R> mapper) {
        Optional<T> byIdOpt = findById(id);
        return byIdOpt.map(mapper);
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(db.find(entityClass(), id));
    }

    @Override
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
    public <R> PagedList<R> pageOffset(String queryPart, int offset, int size, String order, Function<T, R> mapper) {
        PagedList<T> list = pageOffset(queryPart, offset, size, order);
        return new PagedList<>(list.list().stream().map(mapper).collect(Collectors.toList()), list.totalCount());
    }

    @Override
    public <R> List<R> all(Function<T, R> mapper) {
        return all().stream().map(mapper).collect(Collectors.toList());
    }

    @Override
    public List<T> all() {
        return db.find(entityClass()).findList();
    }
}
