package com.github.brane08.pagila.seedworks.infra.query;

import javax.persistence.criteria.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

import static com.github.brane08.pagila.seedworks.infra.query.QueryParser.*;

public final class QueryExpr {

	public static final QueryExpr NO_OP = new QueryExpr("", "", "");

	private final String field;
	private final String operator;
	private final String value;

	public QueryExpr(String field, String operator, String value) {
		this.field = field.trim();
		this.operator = operator.isBlank() ? "=" : operator.trim();
		this.value = value.trim();
	}

	public QueryExpr(String[] args) {
		this(List.of(args));
	}

	public QueryExpr(List<String> args) {
		this(args.get(0), args.get(1), args.get(2));
	}

	public String getField() {
		return field;
	}

	public String getOperator() {
		return operator;
	}

	public String getValue() {
		return value;
	}

	<X> Predicate toPredicate(CriteriaBuilder cb, Root<X> root) throws ParseException {
		Path<?> path = root.get(getField());

		if (OP_EQ.equalsIgnoreCase(getOperator())) {
			return cb.equal(path, getValue());
		} else if (OP_LT.equalsIgnoreCase(getOperator())) {
			Number value = NumberFormat.getInstance().parse(getValue());
			return cb.lt((Expression<? extends Number>) path, value);
		} else if (OP_GT.equalsIgnoreCase(getOperator())) {
			Number value = NumberFormat.getInstance().parse(getValue());
			return cb.lt((Expression<? extends Number>) path, value);
		}
		return null;
	}

	@Override
	public String toString() {
		return "QueryExpr{" +
				"field='" + field + '\'' +
				", operator='" + operator + '\'' +
				", value='" + value + '\'' +
				'}';
	}
}
