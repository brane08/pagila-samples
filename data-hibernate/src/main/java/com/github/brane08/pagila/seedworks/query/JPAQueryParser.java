package com.github.brane08.pagila.seedworks.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JPAQueryParser<T> extends QueryParser<T> {

    private final EntityManager em;
    private final CriteriaBuilder cb;

    public JPAQueryParser(EntityManager em, String queryString, Class<T> resultClass) {
        super(queryString, resultClass);
        this.em = em;
        this.cb = em.getCriteriaBuilder();
    }

    @Override
    public List<T> getResults(int offset, int size, String order) {
        CriteriaQuery<T> query = cb.createQuery(this.resultClass());
        Root<T> root = query.from(this.resultClass());
        query.select(root);
        applyWhere(query, root);
        query.orderBy(toOrders(root, order));
        TypedQuery<T> typedQuery = this.em.createQuery(query);
        return typedQuery.setFirstResult(offset).setMaxResults(size).getResultList();
    }

    @Override
    public int getCount() {
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(this.resultClass());
        query.select(cb.count(root));
        applyWhere(query, root);
        return this.em.createQuery(query).getSingleResult().intValue();
    }

    private void applyWhere(CriteriaQuery<?> query, Root<T> root) {
        List<Predicate> predicates = new ArrayList<>();
        try {
            for (QueryExpr expr : exprs()) {
                predicates.add(toPredicate(cb, root, expr));
            }
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(Predicate[]::new)));
        }
    }

    /** Comma separated fields; a leading '-' means descending. Empty order leaves ordering to the database. */
    private List<Order> toOrders(Root<T> root, String order) {
        Objects.requireNonNull(order, "Sort order should not be null");
        List<Order> orders = new ArrayList<>();
        for (String item : order.split(",")) {
            if (item.isEmpty()) {
                continue;
            }
            orders.add(item.startsWith("-") ? cb.desc(field(root, item.substring(1))) : cb.asc(field(root, item)));
        }
        return orders;
    }

    private static <X> Path<?> field(Root<X> root, String name) {
        try {
            return root.get(name);
        } catch (IllegalArgumentException ex) {
            throw new InvalidQueryException(name, ex);
        }
    }

    <X> Predicate toPredicate(CriteriaBuilder cb, Root<X> root, QueryExpr expr) throws ParseException {
        Path<?> path = field(root, expr.field());

        if (QueryExpr.OP_EQ.equalsIgnoreCase(expr.operator())) {
            return cb.equal(path, expr.value());
        } else if (QueryExpr.OP_LT.equalsIgnoreCase(expr.operator())) {
            Number value = NumberFormat.getInstance().parse(expr.value());
            return cb.lt((Expression<? extends Number>) path, value);
        } else if (QueryExpr.OP_GT.equalsIgnoreCase(expr.operator())) {
            Number value = NumberFormat.getInstance().parse(expr.value());
            return cb.gt((Expression<? extends Number>) path, value);
        }
        throw new InvalidQueryException("Unknown operator: " + expr.operator());
    }
}
