package com.github.brane08.pagila.seedworks.query;

import io.ebean.Database;
import io.ebean.Junction;
import io.ebean.OrderBy;
import io.ebean.Query;
import io.ebean.plugin.BeanType;
import io.ebean.plugin.SpiServer;

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
        if (size <= 0) {
            return List.of();
        }
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
            appendCriteria(where, expr);
        }
    }

    /** Unknown filter/sort properties are a client error (400), not an Ebean failure at query time (500). */
    private void requireKnownProperty(String name) {
        SpiServer server = db.pluginApi();
        BeanType<T> type = server == null ? null : server.beanType(resultClass());
        if (type != null && type.property(name) == null) {
            throw new InvalidQueryException(name, null);
        }
    }

    void appendCriteria(final Junction<T> where, final QueryExpr expr) {
        if (QueryExpr.OP_EQ.equalsIgnoreCase(expr.operator()) || QueryExpr.OP_LT.equalsIgnoreCase(expr.operator())
                || QueryExpr.OP_GT.equalsIgnoreCase(expr.operator())) {
            requireKnownProperty(expr.field());
        }
        if (QueryExpr.OP_EQ.equalsIgnoreCase(expr.operator())) {
            where.eq(expr.field(), expr.value());
        } else if (QueryExpr.OP_LT.equalsIgnoreCase(expr.operator())) {
            where.lt(expr.field(), parseNumber(expr.field(), expr.value()));
        } else if (QueryExpr.OP_GT.equalsIgnoreCase(expr.operator())) {
            where.gt(expr.field(), parseNumber(expr.field(), expr.value()));
        } else {
            throw new InvalidQueryException("Unknown operator: " + expr.operator());
        }
    }

    void appendSort(final Query<T> query, String order) {
        Objects.requireNonNull(order, "Sort order should not be null");
        List<String> items = splitExcludeEmpty(order);
        if (!items.isEmpty()) {
            OrderBy<T> oBy = query.orderBy();
            for (String item : items) {
                if (item.startsWith("-")) {
                    requireKnownProperty(item.substring(1));
                    oBy.desc(item.substring(1));
                } else {
                    requireKnownProperty(item);
                    oBy.asc(item);
                }
            }
        }
    }

    private List<String> splitExcludeEmpty(String input) {
        return Arrays.stream(input.split(",")).filter(item -> !item.isEmpty() && !item.equals("-"))
                .collect(Collectors.toList());
    }
}
