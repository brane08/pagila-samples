package com.github.brane08.pagila.seedworks.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JPAQueryParserTest {

    private EntityManager em;
    private CriteriaBuilder cb;
    private CriteriaQuery<Actor> selectQuery;
    private CriteriaQuery<Long> countQuery;
    private Root<Actor> root;
    private TypedQuery<Actor> typedQuery;
    private TypedQuery<Long> countTyped;
    private Path<Object> titlePath;
    private Predicate combined;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        cb = mock(CriteriaBuilder.class);
        selectQuery = mock(CriteriaQuery.class);
        countQuery = mock(CriteriaQuery.class);
        root = mock(Root.class);
        typedQuery = mock(TypedQuery.class, RETURNS_SELF);
        countTyped = mock(TypedQuery.class);
        titlePath = mock(Path.class);
        combined = mock(Predicate.class);

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Actor.class)).thenReturn(selectQuery);
        when(cb.createQuery(Long.class)).thenReturn(countQuery);
        when(selectQuery.from(Actor.class)).thenReturn(root);
        when(countQuery.from(Actor.class)).thenReturn(root);
        when(em.createQuery(selectQuery)).thenReturn(typedQuery);
        when(em.createQuery(countQuery)).thenReturn(countTyped);
        when(countTyped.getSingleResult()).thenReturn(42L);
        when(typedQuery.getResultList()).thenReturn(List.of());
        doReturn(mock(Expression.class)).when(cb).count(root);
        doReturn(titlePath).when(root).get("title");
        doReturn(titlePath).when(root).get("length");
        when(cb.and(any(Predicate[].class))).thenReturn(combined);
    }

    private JPAQueryParser<Actor> parser(String queryString) {
        return new JPAQueryParser<>(em, queryString, Actor.class);
    }

    @Test
    void eqOperatorBuildsCbEqualOnField() throws Exception {
        Predicate eq = mock(Predicate.class);
        when(cb.equal(titlePath, "ACADEMY")).thenReturn(eq);

        Predicate result = parser("").toPredicate(cb, root, new QueryExpr("title", QueryExpr.OP_EQ, "ACADEMY"));

        assertSame(eq, result);
        verify(root).get("title");
    }

    @Test
    void ltOperatorBuildsCbLtWithParsedNumber() throws Exception {
        Predicate lt = mock(Predicate.class);
        when(cb.lt(any(Expression.class), any(Number.class))).thenReturn(lt);

        Predicate result = parser("").toPredicate(cb, root, new QueryExpr("length", QueryExpr.OP_LT, "60"));

        assertSame(lt, result);
        ArgumentCaptor<Number> value = ArgumentCaptor.forClass(Number.class);
        verify(cb).lt(any(Expression.class), value.capture());
        assertEquals(60, value.getValue().intValue());
        verify(cb, never()).gt(any(Expression.class), any(Number.class));
        verify(root).get("length");
    }

    @Test
    void gtOperatorBuildsCbGtWithParsedNumber() throws Exception {
        Predicate gt = mock(Predicate.class);
        when(cb.gt(any(Expression.class), any(Number.class))).thenReturn(gt);

        Predicate result = parser("").toPredicate(cb, root, new QueryExpr("length", QueryExpr.OP_GT, "90"));

        assertSame(gt, result);
        ArgumentCaptor<Number> value = ArgumentCaptor.forClass(Number.class);
        verify(cb).gt(any(Expression.class), value.capture());
        assertEquals(90, value.getValue().intValue());
        verify(cb, never()).lt(any(Expression.class), any(Number.class));
    }

    @Test
    void unknownOperatorYieldsNoPredicate() throws Exception {
        assertNull(parser("").toPredicate(cb, root, new QueryExpr("title", "like", "ACA")));
    }

    @Test
    void getCountAppliesWherePredicatesAndCountsRoot() throws Exception {
        Predicate eqP = mock(Predicate.class);
        Predicate gtP = mock(Predicate.class);
        when(cb.equal(titlePath, "ACADEMY")).thenReturn(eqP);
        when(cb.gt(any(Expression.class), any(Number.class))).thenReturn(gtP);

        int count = parser("title==ACADEMY;length=gt=60").getCount();

        assertEquals(42, count);
        verify(cb).count(root);
        verify(countQuery).select(any());
        verify(cb).and(new Predicate[] {eqP, gtP});
        verify(countQuery).where(combined);
    }

    @Test
    void getCountWithoutFiltersSkipsWhere() {
        assertEquals(42, parser("").getCount());

        verify(cb).count(root);
        verify(countQuery, never()).where(any(Expression.class));
    }

    @Test
    void getCountWrapsNumericParseErrorInRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> parser("length=lt=abc").getCount());

        assertInstanceOf(java.text.ParseException.class, ex.getCause());
    }

    @Test
    void getResultsAppliesPagingAndReturnsList() {
        List<Actor> expected = List.of(new Actor());
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Actor> result = parser("").getResults(5, 10, "");

        assertSame(expected, result);
        verify(typedQuery).setFirstResult(5);
        verify(typedQuery).setMaxResults(10);
    }

    @Test
    void getResultsFromPageInfoUsesOffsetAndSize() {
        PageInfo info = new PageInfo(2, 10, "");

        parser("").getResults(info);

        verify(typedQuery).setFirstResult(info.offset());
        verify(typedQuery).setMaxResults(10);
    }

    @Test
    void ascendingOrderUsesCbAsc() {
        Order asc = mock(Order.class);
        when(cb.asc(titlePath)).thenReturn(asc);

        parser("").getResults(0, 5, "title");

        verify(cb).asc(titlePath);
        verify(selectQuery).orderBy(List.of(asc));
    }

    @Test
    void leadingMinusUsesCbDesc() {
        Order desc = mock(Order.class);
        when(cb.desc(titlePath)).thenReturn(desc);

        parser("").getResults(0, 5, "-title");

        verify(cb).desc(titlePath);
        verify(cb, never()).asc(any());
        verify(selectQuery).orderBy(List.of(desc));
    }

    @Test
    void mixedOrderKeepsFieldOrderAndSkipsEmptyItems() {
        Path<Object> lengthPath = mock(Path.class);
        doReturn(lengthPath).when(root).get("length");
        Order asc = mock(Order.class);
        Order desc = mock(Order.class);
        when(cb.asc(titlePath)).thenReturn(asc);
        when(cb.desc(lengthPath)).thenReturn(desc);

        parser("").getResults(0, 5, "title,,-length");

        verify(selectQuery).orderBy(List.of(asc, desc));
    }

    @Test
    void emptyOrderLeavesOrderingToDatabase() {
        parser("").getResults(0, 5, "");

        verify(selectQuery).orderBy(List.of());
    }

    @Test
    void nullOrderThrowsNullPointerException() {
        JPAQueryParser<Actor> p = parser("");

        assertThrows(NullPointerException.class, () -> p.getResults(0, 5, null));
    }
}
