package com.github.brane08.pagila.seedworks.query;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class QueryParser<T> {

    protected final List<QueryExpr> exprs;
    protected final Class<T> resultClass;

    protected QueryParser(String queryString, Class<T> resultClass) {
        this.exprs = toQueryExprs(queryString);
        this.resultClass = resultClass;
    }

    protected List<QueryExpr> exprs() {
        return exprs;
    }

    protected Class<T> resultClass() {
        return resultClass;
    }

    protected List<QueryExpr> toQueryExprs(String queryPart) {
        if (StringUtils.isBlank(queryPart)) {
            return Collections.emptyList();
        }
        return Arrays.stream(queryPart.split(";")).map(item -> {
            String[] args = item.split("=");
            return new QueryExpr(args);
        }).toList();
    }

    public abstract List<T> getResults(PageInfo request);

    public abstract int getCount();
}
