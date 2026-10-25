package com.github.brane08.pagila.seedworks.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.Junction;
import io.ebean.OrderBy;
import io.ebean.Query;
import java.text.ParseException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EbeanQueryParserTest {

    @Mock
    Database db;

    @Mock(answer = Answers.RETURNS_SELF)
    Query<Actor> query;

    @Mock
    ExpressionList<Actor> where;

    @Mock
    Junction<Actor> and;

    @Mock
    Junction<Actor> or;

    @Mock
    OrderBy<Actor> orderBy;

    @BeforeEach
    void setUp() {
        when(db.createQuery(Actor.class)).thenReturn(query);
        when(query.where()).thenReturn(where);
        when(where.and()).thenReturn(and);
        when(where.or()).thenReturn(or);
        when(query.orderBy()).thenReturn(orderBy);
        when(query.findCount()).thenReturn(42);
        when(query.findList()).thenReturn(List.of());
    }

    private EbeanQueryParser<Actor> parser(String queryString) {
        return new EbeanQueryParser<>(db, queryString, Actor.class);
    }

    // ---- criteria ----

    @Test
    void eqCriteriaUsesJunctionEqWithRawValue() {
        parser("title==ACADEMY").getCount();

        verify(and).eq("title", "ACADEMY");
        verify(and, never()).lt(anyString(), any(Object.class));
        verify(and, never()).gt(anyString(), any(Object.class));
    }

    @Test
    void ltCriteriaParsesNumericValue() {
        parser("length=lt=60").getCount();

        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(and).lt(eq("length"), value.capture());
        assertEquals(60, ((Number) value.getValue()).intValue());
        verify(and, never()).gt(anyString(), any(Object.class));
    }

    @Test
    void gtCriteriaParsesNumericValue() {
        parser("length=gt=90").getCount();

        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(and).gt(eq("length"), value.capture());
        assertEquals(90, ((Number) value.getValue()).intValue());
        verify(and, never()).lt(anyString(), any(Object.class));
    }

    @Test
    void operatorMatchingIsCaseInsensitive() throws ParseException {
        EbeanQueryParser<Actor> p = parser("");

        p.appendCriteria(and, new QueryExpr("length", "LT", "60"));
        p.appendCriteria(and, new QueryExpr("length", "Gt", "90"));

        verify(and).lt(eq("length"), any(Object.class));
        verify(and).gt(eq("length"), any(Object.class));
    }

    @Test
    void ltWithNonNumericValueThrowsRuntimeExceptionCausedByParseException() {
        EbeanQueryParser<Actor> p = parser("length=lt=abc");

        RuntimeException ex = assertThrows(RuntimeException.class, p::getCount);

        assertInstanceOf(ParseException.class, ex.getCause());
    }

    @Test
    void gtWithNonNumericValueThrowsRuntimeExceptionCausedByParseException() {
        EbeanQueryParser<Actor> p = parser("length=gt=xyz");

        RuntimeException ex = assertThrows(RuntimeException.class, p::getCount);

        assertInstanceOf(ParseException.class, ex.getCause());
    }

    @Test
    void unsupportedOperatorIsIgnored() throws ParseException {
        parser("").appendCriteria(and, new QueryExpr("title", "like", "ACA"));

        verifyNoInteractions(and);
    }

    @Test
    void emptyFilterAddsNoCriteria() {
        assertEquals(42, parser("").getCount());

        verifyNoInteractions(and, or);
        verify(query).findCount();
    }

    @Test
    void addPredicatesUsesAndJunctionForAndOperator() {
        EbeanQueryParser<Actor> p = parser("");

        p.addPredicates(query, "and");
        p.addPredicates(query, "AND");

        verify(where, times(2)).and();
        verify(where, never()).or();
    }

    @Test
    void addPredicatesUsesOrJunctionForOrOperator() {
        parser("title==ACADEMY").addPredicates(query, "or");

        verify(where).or();
        verify(where, never()).and();
        verify(or).eq("title", "ACADEMY");
    }

    @Test
    void getCountReturnsFindCountResult() {
        assertEquals(42, parser("length=gt=10").getCount());

        verify(query).findCount();
        verify(query, never()).findList();
    }

    // ---- paging ----

    @Test
    void getResultsSetsFirstRowAndMaxRowsAndReturnsList() {
        List<Actor> expected = List.of(new Actor());
        when(query.findList()).thenReturn(expected);

        List<Actor> result = parser("").getResults(5, 10, "");

        assertSame(expected, result);
        verify(query).setFirstRow(5);
        verify(query).setMaxRows(10);
    }

    @Test
    void getResultsFromPageInfoUsesOffsetAndSize() {
        parser("").getResults(new PageInfo(2, 10, ""));

        verify(query).setFirstRow(10);
        verify(query).setMaxRows(10);
    }

    // ---- sorting ----

    @Test
    void ascendingSortUsesOrderByAsc() {
        parser("").getResults(0, 5, "title");

        verify(orderBy).asc("title");
        verify(orderBy, never()).desc(anyString());
    }

    @Test
    void leadingMinusUsesOrderByDesc() {
        parser("").getResults(0, 5, "-title");

        verify(orderBy).desc("title");
        verify(orderBy, never()).asc(anyString());
    }

    @Test
    void mixedSortKeepsFieldOrderAndSkipsEmptyItems() {
        parser("").getResults(0, 5, "title,,-length");

        verify(orderBy).asc("title");
        verify(orderBy).desc("length");
        verifyNoMoreInteractions(orderBy);
    }

    @Test
    void emptyOrderAddsNoOrdering() {
        parser("").getResults(0, 5, "");

        verify(query, never()).orderBy();
        verifyNoInteractions(orderBy);
    }

    @Test
    void blankSegmentsOnlyAddNoOrdering() {
        parser("").getResults(0, 5, ",,");

        verify(query, never()).orderBy();
        verifyNoInteractions(orderBy);
    }

    @Test
    void nullOrderThrowsNullPointerException() {
        EbeanQueryParser<Actor> p = parser("");

        NullPointerException ex = assertThrows(NullPointerException.class, () -> p.getResults(0, 5, null));

        assertEquals("Sort order should not be null", ex.getMessage());
    }

    @Test
    void filterSortAndPagingCombine() {
        parser("title==ACADEMY;length=lt=120").getResults(20, 10, "-length");

        verify(and).eq("title", "ACADEMY");
        verify(and).lt(eq("length"), any(Object.class));
        verify(orderBy).desc("length");
        verify(query).setFirstRow(20);
        verify(query).setMaxRows(10);
    }
}
