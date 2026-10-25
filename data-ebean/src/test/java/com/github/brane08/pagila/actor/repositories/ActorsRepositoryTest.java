package com.github.brane08.pagila.actor.repositories;

import com.github.brane08.pagila.actor.beans.ActorViewInfo;
import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.actor.entities.ActorView;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.repositories.Repository;
import com.github.brane08.pagila.testsupport.EbeanQueryStubs;
import io.ebean.Database;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActorsRepositoryTest {

    @Mock
    private Database db;

    private EbeanQueryStubs ebean;
    private ActorsRepository repository;

    @BeforeEach
    void setUp() {
        ebean = new EbeanQueryStubs(db);
        repository = new ActorsRepository(db);
    }

    @Test
    void listActorViews_mapsRowsAndCount() {
        ebean.rows(ActorView.class, List.of(actorView(1, "PENELOPE", "GUINESS", "ACADEMY DINOSAUR")));
        ebean.count(ActorView.class, 200);

        PagedList<ActorViewInfo> result = repository.listActorViews(20, 10, "lastName");

        assertEquals(200, result.totalCount());
        assertEquals(1, result.list().size());
        assertEquals(1L, result.list().getFirst().getActorId());
        assertEquals("PENELOPE", result.list().getFirst().getFirstName());
        assertEquals("ACADEMY DINOSAUR", result.list().getFirst().getFilmInfo());
        verify(ebean.first(ActorView.class)).setFirstRow(20);
        verify(ebean.first(ActorView.class)).setMaxRows(10);
        verify(ebean.first(ActorView.class).orderBy()).asc("lastName");
    }

    @Test
    void listActorViews_descendingOrderPrefix() {
        ebean.count(ActorView.class, 0);

        repository.listActorViews(0, 5, "-lastName");

        verify(ebean.first(ActorView.class).orderBy()).desc("lastName");
    }

    @Test
    void listActorViews_emptyResult() {
        PagedList<ActorViewInfo> result = repository.listActorViews(0, 10, "");

        assertTrue(result.list().isEmpty());
        assertEquals(0, result.totalCount());
    }

    @Test
    void findActorViewById_returnsMappedViewWhenPresent() {
        when(db.find(ActorView.class, 3)).thenReturn(actorView(3, "ED", "CHASE", "ACADEMY DINOSAUR"));

        Optional<ActorViewInfo> found = repository.findActorViewById(3);

        assertTrue(found.isPresent());
        assertEquals(3L, found.get().getActorId());
        assertEquals("ED", found.get().getFirstName());
        assertEquals("CHASE", found.get().getLastName());
    }

    @Test
    void findActorViewById_returnsEmptyWhenAbsent() {
        when(db.find(ActorView.class, 99)).thenReturn(null);

        assertFalse(repository.findActorViewById(99).isPresent());
    }

    @Test
    void findById_returnsActorWhenPresent() {
        Actor actor = actor(1, "PENELOPE", "GUINESS");
        when(db.find(Actor.class, 1)).thenReturn(actor);

        Optional<Actor> found = repository.findById(1);

        assertTrue(found.isPresent());
        assertEquals("GUINESS", found.get().getLastName());
    }

    @Test
    void findById_returnsEmptyWhenAbsent() {
        when(db.find(Actor.class, 2)).thenReturn(null);

        assertFalse(repository.findById(2).isPresent());
    }

    @Test
    void findById_withMapperAppliesMapperWhenPresent() {
        when(db.find(Actor.class, 1)).thenReturn(actor(1, "PENELOPE", "GUINESS"));
        when(db.find(Actor.class, 2)).thenReturn(null);

        assertEquals(Optional.of("PENELOPE"), repository.findById(1, Actor::getFirstName));
        assertEquals(Optional.empty(), repository.findById(2, Actor::getFirstName));
    }

    @Test
    void count_returnsCountQueryResult() {
        ebean.count(Actor.class, 5);

        assertEquals(5, repository.count(""));
    }

    @Test
    void pageOffset_mapsRowsAndCount() {
        ebean.rows(Actor.class, List.of(actor(1, "PENELOPE", "GUINESS")));
        ebean.count(Actor.class, 1);

        PagedList<Actor> page = repository.pageOffset("", 0, 5, "firstName");

        assertEquals(1, page.list().size());
        assertEquals(1, page.totalCount());
        verify(ebean.first(Actor.class)).setFirstRow(0);
        verify(ebean.first(Actor.class)).setMaxRows(5);
        verify(ebean.first(Actor.class).orderBy()).asc("firstName");
    }

    @Test
    void page_appliesPageInfoOffsetAndSize() {
        ebean.rows(Actor.class, List.of(actor(2, "NICK", "WAHLBERG")));
        ebean.count(Actor.class, 3);

        PagedList<Actor> page = repository.page("", new PageInfo(3, 1, "firstName"));

        assertEquals(1, page.list().size());
        assertEquals(3, page.totalCount());
        verify(ebean.first(Actor.class)).setFirstRow(2);
        verify(ebean.first(Actor.class)).setMaxRows(1);
    }

    @Test
    void pageOffset_withMapperAppliesMapper() {
        ebean.rows(Actor.class, List.of(actor(1, "PENELOPE", "GUINESS"), actor(2, "NICK", "WAHLBERG")));
        ebean.count(Actor.class, 2);

        PagedList<String> page = repository.pageOffset("", 0, 10, "firstName", Actor::getLastName);

        assertEquals(List.of("GUINESS", "WAHLBERG"), page.list());
        assertEquals(2, page.totalCount());
    }

    @Test
    void all_returnsRowsCappedAtMaxPageSize() {
        ebean.rows(Actor.class, List.of(actor(1, "PENELOPE", "GUINESS")));

        assertEquals(1, repository.all().size());
        verify(ebean.first(Actor.class)).setMaxRows(Repository.MAX_PAGE_SIZE);
    }

    @Test
    void all_withMapperAppliesMapper() {
        ebean.rows(Actor.class, List.of(actor(1, "PENELOPE", "GUINESS"), actor(2, "NICK", "WAHLBERG")));

        assertEquals(List.of("PENELOPE", "NICK"), repository.all(Actor::getFirstName));
    }

    @Test
    void all_emptyTable() {
        assertTrue(repository.all().isEmpty());
    }

    @Test
    void facets_defaultIsEmpty() {
        assertTrue(repository.facets("").isEmpty());
    }

    @Test
    void exposesEntityClassFilterPropertyAndDefaultLoadFields() {
        assertEquals(Actor.class, repository.entityClass());
        assertEquals("firstName", repository.filterProperty());
        var probe = new ActorsRepository(db) {
            List<String> loadFields() {
                return getLoadFields();
            }
        };
        assertTrue(probe.loadFields().isEmpty());
    }

    private static Actor actor(int id, String first, String last) {
        Actor actor = new Actor();
        actor.setActorId(id);
        actor.setFirstName(first);
        actor.setLastName(last);
        return actor;
    }

    private static ActorView actorView(int id, String first, String last, String films) {
        ActorView view = new ActorView();
        view.setActorId(id);
        view.setFirstName(first);
        view.setLastName(last);
        view.setFilmInfo(films);
        return view;
    }
}
