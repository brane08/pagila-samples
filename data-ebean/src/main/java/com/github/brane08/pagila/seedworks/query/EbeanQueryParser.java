package com.github.brane08.pagila.seedworks.query;

import io.ebean.Database;
import io.ebean.Junction;
import io.ebean.OrderBy;
import io.ebean.Query;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class EbeanQueryParser<T> extends QueryParser<T> {

    private final Database db;

    public EbeanQueryParser(Database db, String queryString, Class<T> resultClass) {
        super(queryString, resultClass);
        this.db = db;
    }

    @Override
    public List<T> getResults(int offset, int size, String order) {
        Query<T> query = db.createQuery(this.resultClass());
        addPredicates(query, QueryExpr.OP_AND);
        appendSort(query, order);
        return query.setFirstRow(offset).setMaxRows(size).findList();
    }

    @Override
    public int getCount() {
        Query<T> query = db.createQuery(this.resultClass());
        addPredicates(query, QueryExpr.OP_AND);
        return query.findCount();
    }

    void addPredicates(final Query<T> query, final String op) {
        Junction<T> where = (QueryExpr.OP_AND.equalsIgnoreCase(op)) ? query.where().and() : query.where().or();
        for (QueryExpr expr : exprs()) {
            try {
                appendCriteria(where, expr);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void appendCriteria(final Junction<T> where, final QueryExpr expr) throws ParseException {
        if (QueryExpr.OP_EQ.equalsIgnoreCase(expr.operator())) {
            where.eq(expr.field(), expr.value());
        } else if (QueryExpr.OP_LT.equalsIgnoreCase(expr.operator())) {
            Number value = NumberFormat.getInstance().parse(expr.value());
            where.lt(expr.field(), value);
        } else if (QueryExpr.OP_GT.equalsIgnoreCase(expr.operator())) {
            Number value = NumberFormat.getInstance().parse(expr.value());
            where.gt(expr.field(), value);
        }
    }

    void appendSort(final Query<T> query, String order) {
        Objects.requireNonNull(order, "Sort order should not be null");
        List<String> items = splitExcludeEmpty(order);
        if (!items.isEmpty()) {
            OrderBy<T> oBy = query.orderBy();
            for (String item : items) {
                if (item.startsWith("-")) {
                    oBy.desc(item.substring(1));
                } else {
                    oBy.asc(item);
                }
            }
        } else {
            query.orderBy().asc("id");
        }
    }

    private List<String> splitExcludeEmpty(String input) {
        return Arrays.stream(input.split(",")).filter(item -> (item != null && !item.isEmpty()))
                .collect(Collectors.toList());
    }
}
