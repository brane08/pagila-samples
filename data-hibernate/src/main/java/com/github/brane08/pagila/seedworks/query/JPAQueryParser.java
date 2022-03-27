package com.github.brane08.pagila.seedworks.query;

import com.github.brane08.pagila.seedworks.beans.PageInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public final class JPAQueryParser<T> extends QueryParser<T> {

    private final EntityManager em;
    private final CriteriaBuilder cb;

    public JPAQueryParser(EntityManager em, String queryString, Class<T> resultClass) {
        super(queryString, resultClass);
        this.em = em;
        this.cb = em.getCriteriaBuilder();
    }

    @Override
    public List<T> getResults(PageInfo request) {
        CriteriaQuery<T> query = cb.createQuery(this.resultClass());
        Root<T> root = query.from(this.resultClass());
        query.select(root);
        addPredicates(root, query, QueryExpr.OP_AND);
        TypedQuery<T> typedQuery = this.em.createQuery(query);
        return typedQuery.setFirstResult(request.offset()).setMaxResults(request.size()).getResultList();
    }

    @Override
    public int getCount() {
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(this.resultClass());
        query.select(cb.countDistinct(root));
        TypedQuery<Long> typedQuery = this.em.createQuery(query);
        return typedQuery.getResultList().stream().findAny().orElse(0L).intValue();
    }

    private void addPredicates(Root<T> root, CriteriaQuery<T> query, String op) {
        List<Predicate> predicates = new ArrayList<>();
        try {
            for (QueryExpr expr : exprs()) {
                predicates.add(toPredicate(cb, root, expr));
            }
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
        if (!predicates.isEmpty()) {
            if (QueryExpr.OP_AND.equalsIgnoreCase(op)) {
                query.where(cb.and(predicates.toArray(Predicate[]::new)));
            } else {
                query.where(cb.or(predicates.toArray(Predicate[]::new)));
            }
        }
    }

    <X> Predicate toPredicate(CriteriaBuilder cb, Root<X> root, QueryExpr expr) throws ParseException {
        Path<?> path = root.get(expr.field());

        if (QueryExpr.OP_EQ.equalsIgnoreCase(expr.operator())) {
            return cb.equal(path, expr.value());
        } else if (QueryExpr.OP_LT.equalsIgnoreCase(expr.operator())) {
            Number value = NumberFormat.getInstance().parse(expr.value());
            return cb.lt((Expression<? extends Number>) path, value);
        } else if (QueryExpr.OP_GT.equalsIgnoreCase(expr.operator())) {
            Number value = NumberFormat.getInstance().parse(expr.value());
            return cb.lt((Expression<? extends Number>) path, value);
        }
        return null;
    }
}
