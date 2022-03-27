package com.github.brane08.pagila.seedworks.infra.query;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QueryParser<T> {

	public static final String OP_LT = "lt";
	public static final String OP_GT = "gt";
	public static final String OP_EQ = "=";
	public static final String OP_LK = "like";
	public static final String OP_NE = "!";
	public static final String OP_IN = "in";
	public static final String OP_OUT = "out";
	public static final String OP_AND = "and";
	public static final String OP_OR = "or";

	private final EntityManager em;
	private final String filter;
	private final Class<T> resultClass;

	public QueryParser(EntityManager em, Class<T> resultClass, String filter) {
		this.em = em;
		this.resultClass = resultClass;
		this.filter = filter;
	}

	public TypedQuery<T> buildQuery() {
		if (filter.contains(";") && filter.contains(","))
			throw new IllegalArgumentException("Query cannot contain & and | together for now.");
		List<String> toParse = List.of(filter.split("[;,]"));
		String op = (filter.contains(",")) ? "or" : "and";
		System.out.println("Size: " + toParse.size());
		CriteriaBuilder cb = this.em.getCriteriaBuilder();
		CriteriaQuery<T> query = cb.createQuery(this.resultClass);
		Root<T> root = query.from(this.resultClass);
		query.select(root);
		List<Predicate> predicates = tackleIndividualConditions(cb, root, toParse);
		addPredicates(cb, query, op, predicates);
		return em.createQuery(query);
	}

	public TypedQuery<Long> buildCountQuery() {
		if (filter.contains(";") && filter.contains(","))
			throw new IllegalArgumentException("Query cannot contain & and | together for now.");
		List<String> toParse = List.of(filter.split("[;,]"));
		String op = (filter.contains(",")) ? "or" : "and";
		System.out.println("Size: " + toParse.size());
		CriteriaBuilder cb = this.em.getCriteriaBuilder();
		CriteriaQuery<Long> query = cb.createQuery(Long.class);
		Root<T> root = query.from(this.resultClass);
		query.select(cb.countDistinct(root));
		List<Predicate> predicates = tackleIndividualConditions(cb, root, toParse);
		addPredicates(cb, query, op, predicates);
		return em.createQuery(query);
	}

	private <X> void addPredicates(CriteriaBuilder cb, CriteriaQuery<X> query, String op, List<Predicate> predicates) {
		if (!predicates.isEmpty()) {
			if (OP_OR.equalsIgnoreCase(op)) {
				query.where(cb.and(predicates.toArray(Predicate[]::new)));
			} else {
				query.where(cb.or(predicates.toArray(Predicate[]::new)));
			}
		}
	}

	private List<Predicate> tackleIndividualConditions(CriteriaBuilder cb, Root<T> root, List<String> conditions) {
		if (conditions.isEmpty())
			return Collections.emptyList();
		List<QueryExpr> list = new ArrayList<>();
		for (String condition : conditions) {
			String[] args = condition.split("=");
			if (args.length > 2)
				list.add(new QueryExpr(args));
		}
		System.out.println("Parsed: " + list);
		List<Predicate> predicates = new ArrayList<>();
		try {
			for (QueryExpr expr : list) {
				predicates.add(expr.toPredicate(cb, root));
			}
		} catch (ParseException ex) {
			throw new RuntimeException(ex);
		}
		return predicates;
	}
}
