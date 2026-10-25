package com.github.brane08.pagila.testsupport;

import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.Junction;
import io.ebean.OrderBy;
import io.ebean.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test-only fluent stub for the Ebean query API ({@code Database.find}/{@code createQuery} chains).
 * Every query created through the database is a Mockito mock whose fluent methods return the same
 * mock, so production chains like {@code where().and()}, {@code orderBy().asc()},
 * {@code setFirstRow().setMaxRows().findList()} run without a database.
 * Result rows and counts are keyed by the entity class being queried.
 */
public final class EbeanQueryStubs {

    private final Map<Class<?>, List<?>> rows = new HashMap<>();
    private final Map<Class<?>, Integer> counts = new HashMap<>();
    private final Map<Class<?>, List<Query<?>>> created = new HashMap<>();

    public EbeanQueryStubs(Database db) {
        doAnswer(inv -> newQuery(inv.getArgument(0))).when(db).createQuery(any(Class.class));
        doAnswer(inv -> newQuery(inv.getArgument(0))).when(db).find(any(Class.class));
    }

    /** Rows returned by {@code findList()} / {@code findOne()} for the given entity class. */
    public <T> void rows(Class<T> type, List<T> list) {
        rows.put(type, list);
    }

    /** Value returned by {@code findCount()} for the given entity class. */
    public void count(Class<?> type, int value) {
        counts.put(type, value);
    }

    /** All queries created for the given entity class, in creation order. */
    public List<Query<?>> created(Class<?> type) {
        return created.getOrDefault(type, List.of());
    }

    /** The first query created for the entity class (typically the result-list query). */
    @SuppressWarnings("unchecked")
    public <T> Query<T> first(Class<T> type) {
        return (Query<T>) created(type).getFirst();
    }

    /** The last query created for the entity class (typically the count query). */
    @SuppressWarnings("unchecked")
    public <T> Query<T> last(Class<T> type) {
        return (Query<T>) created(type).getLast();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Query<Object> newQuery(Class<?> type) {
        Query<Object> query = mock(Query.class);
        ExpressionList<Object> where = mock(ExpressionList.class);
        Junction<Object> junction = mock(Junction.class);
        OrderBy<Object> orderBy = mock(OrderBy.class);

        doReturn(where).when(query).where();
        doReturn(junction).when(where).and();
        doReturn(junction).when(where).or();
        doReturn(orderBy).when(query).orderBy();
        doReturn(query).when(query).setFirstRow(anyInt());
        doReturn(query).when(query).setMaxRows(anyInt());
        doAnswer(inv -> rowsOf(type)).when(query).findList();
        doAnswer(inv -> countOf(type)).when(query).findCount();
        doAnswer(inv -> rowsOf(type).isEmpty() ? null : rowsOf(type).getFirst()).when(query).findOne();

        created.computeIfAbsent(type, k -> new ArrayList<>()).add(query);
        return query;
    }

    private List<?> rowsOf(Class<?> type) {
        return rows.getOrDefault(type, List.of());
    }

    private int countOf(Class<?> type) {
        return counts.getOrDefault(type, 0);
    }
}
