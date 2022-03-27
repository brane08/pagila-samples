package com.github.brane08.pagila.seedworks.beans;

import java.util.List;

public record PagedList<T>(List<T> list, int totalCount) {

    public int totalPageCount() {
        return Math.round(totalCount / list.size());
    }
}
