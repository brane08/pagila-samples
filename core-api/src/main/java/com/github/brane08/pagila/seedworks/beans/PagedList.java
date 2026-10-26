package com.github.brane08.pagila.seedworks.beans;

import java.util.List;

public record PagedList<T>(List<T> list, int totalCount) {

    public int totalPageCount() {
        return totalPageCount(list.size());
    }

    public int totalPageCount(int pageSize) {
        return pageSize <= 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
    }
}
