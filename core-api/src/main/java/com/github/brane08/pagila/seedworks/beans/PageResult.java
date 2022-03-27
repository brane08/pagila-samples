package com.github.brane08.pagila.seedworks.beans;

import java.util.List;

public class PageResult<T> extends ApiResult<List<T>> {
    private final int totalCount;

    public PageResult(List<T> data, int totalCount) {
        super(true, data);
        this.totalCount = totalCount;
    }

    public int getTotalCount() {
        return totalCount;
    }
}
