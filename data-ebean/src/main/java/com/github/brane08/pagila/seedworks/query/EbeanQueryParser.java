package com.github.brane08.pagila.seedworks.query;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import io.ebean.Database;
import io.ebean.Junction;
import io.ebean.Query;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

public final class EbeanQueryParser<T> extends QueryParser<T> {

    private final Database db;

    public EbeanQueryParser(Database db, String queryString, Class<T> resultClass) {
        super(queryString, resultClass);
        this.db = db;
    }

    @Override
    public List<T> getResults(PageInfo request) {
        Query<T> query = db.createQuery(this.resultClass());
        addPredicates(query, QueryExpr.OP_AND);
        return query.setFirstRow(request.offset()).setMaxRows(request.size()).findList();
    }

    @Override
    public int getCount() {
        Query<T> query = db.createQuery(this.resultClass());
        addPredicates(query, QueryExpr.OP_AND);
        return query.findCount();
    }

    private void addPredicates(Query<T> query, String op) {
        Junction<T> where = (QueryExpr.OP_AND.equalsIgnoreCase(op)) ? query.where().and() : query.where().or();
        for (QueryExpr expr : exprs()) {
            try {
                appendCriteria(where, expr);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void appendCriteria(Junction<T> where, QueryExpr expr) throws ParseException {
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
}
