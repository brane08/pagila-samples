package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.Facet;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface Repository<T, ID> {

    Optional<T> findById(ID id);

    default PagedList<T> page(String queryPart, PageInfo request) {
        return pageOffset(queryPart, request.offset(), request.size(), request.order());
    }

    PagedList<T> pageOffset(String queryPart, int offset, int size, String order);

    int count(String queryPart);

    List<T> all();

    default List<Facet> facets(String queryPart) {
        return Collections.emptyList();
    }
}
