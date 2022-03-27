package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface MappingRepository<T, ID> {

    <R> Optional<R> findById(ID id, Function<T, R> mapper);

    <R> PagedList<R> page(String queryPart, PageInfo request, Function<T, R> mapper);
}
