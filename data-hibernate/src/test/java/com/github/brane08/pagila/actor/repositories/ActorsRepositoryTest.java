package com.github.brane08.pagila.actor.repositories;

import com.github.brane08.pagila.actor.beans.ActorViewInfo;
import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.actor.entities.ActorView;
import com.github.brane08.pagila.film.repositories.JpaQueryStubs;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActorsRepositoryTest {

    @Mock
    private EntityManager em;

    private JpaQueryStubs jpa;
    private ActorsRepository repository;

    @BeforeEach
    void setUp() {
        jpa = new JpaQueryStubs(em);
        repository = new ActorsRepository(em);
    }

    @Test
    void listActorViews_mapsRowsAndCount() {
        ActorView view = new ActorView();
        view.setActorId(5);
        view.setFirstName("ED");
        view.setLastName("CHASE");
        view.setFilmInfo("Action: ACADEMY DINOSAUR");
        jpa.rows(ActorView.class, List.of(view));
        jpa.count(3);

        PagedList<ActorViewInfo> page = repository.listActorViews(0, 20, "lastName");

        assertEquals(3, page.totalCount());
        ActorViewInfo info = page.list().getFirst();
        assertEquals(5L, info.getActorId());
        assertEquals("ED", info.getFirstName());
        assertEquals("CHASE", info.getLastName());
        assertEquals("Action: ACADEMY DINOSAUR", info.getFilmInfo());
        verify(jpa.typed(ActorView.class)).setFirstResult(0);
        verify(jpa.typed(ActorView.class)).setMaxResults(20);
        verify(jpa.cb()).asc(any());
    }

    @Test
    void listActorViews_descendingOrderAndEmptyResult() {
        jpa.rows(ActorView.class, List.of());
        jpa.count(0);

        PagedList<ActorViewInfo> page = repository.listActorViews(40, 20, "-firstName");

        assertTrue(page.list().isEmpty());
        assertEquals(0, page.totalCount());
        verify(jpa.typed(ActorView.class)).setFirstResult(40);
        verify(jpa.cb()).desc(any());
    }

    @Test
    void findActorViewById_returnsMappedViewWhenPresent() {
        ActorView view = new ActorView();
        view.setActorId(7);
        view.setFirstName("JOHNNY");
        view.setLastName("CAGE");
        when(em.find(ActorView.class, 7)).thenReturn(view);

        Optional<ActorViewInfo> found = repository.findActorViewById(7);

        assertTrue(found.isPresent());
        assertEquals(7L, found.get().getActorId());
        assertEquals("JOHNNY", found.get().getFirstName());
    }

    @Test
    void findActorViewById_returnsEmptyWhenAbsent() {
        when(em.find(ActorView.class, 8)).thenReturn(null);

        assertFalse(repository.findActorViewById(8).isPresent());
    }

    @Test
    void findById_delegatesToEntityManager() {
        Actor actor = new Actor();
        actor.setActorId(1);
        when(em.find(Actor.class, 1)).thenReturn(actor);

        assertEquals(Optional.of(actor), repository.findById(1));
    }

    @Test
    void exposesEntityClassAndFilterProperty() {
        assertEquals(Actor.class, repository.entityClass());
        assertEquals("firstName", repository.filterProperty());
    }
}
