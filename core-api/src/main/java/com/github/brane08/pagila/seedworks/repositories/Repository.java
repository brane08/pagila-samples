package com.github.brane08.pagila.seedworks.repositories;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;

import java.util.List;
import java.util.Optional;

public interface Repository<T, ID> {

    Optional<T> findById(ID id);

    PagedList<T> page(String queryPart, PageInfo request);

    int count(String queryPart);
}
