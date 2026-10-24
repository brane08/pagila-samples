package com.github.brane08.pagila.seedworks.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JPARepositoryTest {

    /** Minimal concrete repository over the Actor entity. */
    static class ActorRepository extends JPARepository<Actor, Integer> {

        ActorRepository(EntityManager em) {
            super(em);
        }

        @Override
        protected Class<Actor> entityClass() {
            return Actor.class;
        }

        @Override
        protected String filterProperty() {
            return "lastName";
        }
    }

    private EntityManager em;
    private CriteriaBuilder cb;
    private CriteriaQuery<Actor> selectQuery;
    private CriteriaQuery<Long> countQuery;
    private TypedQuery<Actor> typedQuery;
    private TypedQuery<Long> countTyped;
    private ActorRepository repo;
    private Actor actor;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        cb = mock(CriteriaBuilder.class);
        selectQuery = mock(CriteriaQuery.class);
        countQuery = mock(CriteriaQuery.class);
        typedQuery = mock(TypedQuery.class, RETURNS_SELF);
        countTyped = mock(TypedQuery.class);
        Root<Actor> root = mock(Root.class);

        actor = new Actor();
        actor.setActorId(1);
        actor.setFirstName("PENELOPE");
        actor.setLastName("GUINESS");

        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Actor.class)).thenReturn(selectQuery);
        when(cb.createQuery(Long.class)).thenReturn(countQuery);
        when(selectQuery.from(Actor.class)).thenReturn(root);
        when(countQuery.from(Actor.class)).thenReturn(root);
        doReturn(mock(Expression.class)).when(cb).count(root);
        when(em.createQuery(selectQuery)).thenReturn(typedQuery);
        when(em.createQuery(countQuery)).thenReturn(countTyped);
        when(countTyped.getSingleResult()).thenReturn(42L);
        when(typedQuery.getResultList()).thenReturn(List.of(actor));

        repo = new ActorRepository(em);
    }

    @Test
    void findByIdReturnsEntityWhenPresent() {
        when(em.find(Actor.class, 1)).thenReturn(actor);

        Optional<Actor> found = repo.findById(1);

        assertSame(actor, found.orElseThrow());
    }

    @Test
    void findByIdReturnsEmptyWhenAbsent() {
        assertTrue(repo.findById(99).isEmpty());
    }

    @Test
    void findByIdWithMapperAppliesMapper() {
        when(em.find(Actor.class, 1)).thenReturn(actor);

        Optional<String> name = repo.findById(1, Actor::getFirstName);

        assertEquals(Optional.of("PENELOPE"), name);
    }

    @Test
    void findByIdWithMapperReturnsEmptyWhenAbsent() {
        assertTrue(repo.findById(99, Actor::getFirstName).isEmpty());
    }

    @Test
    void countDelegatesToCountQuery() {
        assertEquals(42, repo.count(""));
    }

    @Test
    void pageOffsetReturnsListAndTotalCount() {
        PagedList<Actor> page = repo.pageOffset("", 0, 10, "");

        assertEquals(List.of(actor), page.list());
        assertEquals(42, page.totalCount());
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(10);
    }

    @Test
    void pageOffsetWithMapperMapsEachEntity() {
        PagedList<String> page = repo.pageOffset("", 0, 10, "", Actor::getLastName);

        assertEquals(List.of("GUINESS"), page.list());
        assertEquals(42, page.totalCount());
    }

    @Test
    void pageWithPageInfoUsesOffsetAndSize() {
        PageInfo request = new PageInfo(2, 5, "");

        PagedList<Actor> page = repo.page("", request);

        assertEquals(List.of(actor), page.list());
        verify(typedQuery).setFirstResult(request.offset());
        verify(typedQuery).setMaxResults(5);
    }

    @Test
    void pageWithMapperMapsEntities() {
        PageInfo request = new PageInfo(1, 5, "");

        PagedList<String> page = repo.page("", request, Actor::getFirstName);

        assertEquals(List.of("PENELOPE"), page.list());
        assertEquals(42, page.totalCount());
    }

    @Test
    void allCapsResultsAtMaxPageSize() {
        List<Actor> all = repo.all();

        assertEquals(List.of(actor), all);
        verify(typedQuery).setMaxResults(Repository.MAX_PAGE_SIZE);
    }

    @Test
    void allWithMapperMapsEveryEntity() {
        assertEquals(List.of("GUINESS"), repo.all(Actor::getLastName));
    }
}
