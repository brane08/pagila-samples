package com.github.brane08.pagila.film.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test-only stubs for the JPA criteria API as used by JPAQueryParser and JPARepository.
 * Result rows are keyed by the result class; count queries are keyed by Long.class.
 * Shared by repository tests in the film, actor and rental packages.
 */
public final class JpaQueryStubs {

    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Map<Object, Class<?>> criteriaTypes = new HashMap<>();
    private final Map<Class<?>, TypedQuery<?>> typedQueries = new HashMap<>();
    private final Map<Class<?>, List<?>> rows = new HashMap<>();
    private long total;

    public JpaQueryStubs(EntityManager em) {
        when(em.getCriteriaBuilder()).thenReturn(cb);
        doAnswer(inv -> {
            Class<?> type = inv.getArgument(0);
            return newCriteria(type);
        }).when(cb).createQuery(any(Class.class));
        doAnswer(inv -> {
            CriteriaQuery<?> cq = inv.getArgument(0);
            return typedFor(cq);
        }).when(em).createQuery(any(CriteriaQuery.class));
    }

    /** Rows returned by the result query for the given entity class. */
    public <T> void rows(Class<T> type, List<T> list) {
        rows.put(type, list);
    }

    /** Value returned by any count query. */
    public void count(long value) {
        this.total = value;
    }

    public CriteriaBuilder cb() {
        return cb;
    }

    /** The TypedQuery created for the most recent query of this result class. */
    @SuppressWarnings("unchecked")
    public <T> TypedQuery<T> typed(Class<T> type) {
        return (TypedQuery<T>) typedQueries.get(type);
    }

    private CriteriaQuery<Object> newCriteria(Class<?> type) {
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> cq = mock(CriteriaQuery.class);
        @SuppressWarnings("unchecked")
        Root<Object> root = mock(Root.class);
        doReturn(root).when(cq).from(any(Class.class));
        doAnswer(inv -> mock(Path.class)).when(root).get(anyString());
        criteriaTypes.put(cq, type);
        return cq;
    }

    private TypedQuery<Object> typedFor(CriteriaQuery<?> cq) {
        Class<?> type = criteriaTypes.get(cq);
        @SuppressWarnings("unchecked")
        TypedQuery<Object> tq = mock(TypedQuery.class);
        when(tq.setFirstResult(anyInt())).thenReturn(tq);
        when(tq.setMaxResults(anyInt())).thenReturn(tq);
        doAnswer(inv -> rows.getOrDefault(type, List.of())).when(tq).getResultList();
        doAnswer(inv -> total).when(tq).getSingleResult();
        typedQueries.put(type, tq);
        return tq;
    }
}
