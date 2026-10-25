package com.github.brane08.pagila.seedworks.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.seedworks.beans.PageInfo;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.EbeanQueryParser;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.Junction;
import io.ebean.OrderBy;
import io.ebean.Query;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EbeanRepositoryTest {

    /** Minimal concrete repository over the Actor entity. */
    static class ActorRepository extends EbeanRepository<Actor, Integer> {

        ActorRepository(Database db) {
            super(db);
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

    @Mock
    Database db;

    @Mock(answer = Answers.RETURNS_SELF)
    Query<Actor> query;

    @Mock
    ExpressionList<Actor> where;

    @Mock
    Junction<Actor> and;

    @Mock
    OrderBy<Actor> orderBy;

    private Actor actor;
    private ActorRepository repo;

    @BeforeEach
    void setUp() {
        actor = new Actor();
        actor.setActorId(1);
        actor.setFirstName("PENELOPE");
        actor.setLastName("GUINESS");

        when(db.createQuery(Actor.class)).thenReturn(query);
        when(db.find(Actor.class)).thenReturn(query);
        when(query.where()).thenReturn(where);
        when(where.and()).thenReturn(and);
        when(query.orderBy()).thenReturn(orderBy);
        when(query.findCount()).thenReturn(42);
        when(query.findList()).thenReturn(List.of(actor));

        repo = new ActorRepository(db);
    }

    @Test
    void findByIdReturnsEntityWhenPresent() {
        when(db.find(Actor.class, 1)).thenReturn(actor);

        Optional<Actor> found = repo.findById(1);

        assertSame(actor, found.orElseThrow());
    }

    @Test
    void findByIdReturnsEmptyWhenAbsent() {
        assertTrue(repo.findById(99).isEmpty());
    }

    @Test
    void findByIdWithMapperAppliesMapper() {
        when(db.find(Actor.class, 1)).thenReturn(actor);

        assertEquals(Optional.of("PENELOPE"), repo.findById(1, Actor::getFirstName));
    }

    @Test
    void findByIdWithMapperReturnsEmptyWhenAbsent() {
        assertTrue(repo.findById(99, Actor::getFirstName).isEmpty());
    }

    @Test
    void countDelegatesToFindCount() {
        assertEquals(42, repo.count(""));

        verify(query).findCount();
    }

    @Test
    void countAppliesFilterCriteria() {
        repo.count("lastName==GUINESS");

        verify(and).eq("lastName", "GUINESS");
    }

    @Test
    void pageOffsetReturnsListAndTotalCount() {
        PagedList<Actor> page = repo.pageOffset("", 0, 10, "");

        assertEquals(List.of(actor), page.list());
        assertEquals(42, page.totalCount());
        verify(query).setFirstRow(0);
        verify(query).setMaxRows(10);
    }

    @Test
    void pageOffsetWithMapperMapsEachEntity() {
        PagedList<String> page = repo.pageOffset("", 0, 10, "", Actor::getLastName);

        assertEquals(List.of("GUINESS"), page.list());
        assertEquals(42, page.totalCount());
    }

    @Test
    void pageOffsetAppliesSortOrder() {
        repo.pageOffset("", 0, 10, "-lastName");

        verify(orderBy).desc("lastName");
    }

    @Test
    void pageWithPageInfoUsesOffsetAndSize() {
        PagedList<Actor> page = repo.page("", new PageInfo(2, 5, ""));

        assertEquals(List.of(actor), page.list());
        verify(query).setFirstRow(5);
        verify(query).setMaxRows(5);
    }

    @Test
    void pageWithMapperMapsEntities() {
        PagedList<String> page = repo.page("", new PageInfo(1, 5, ""), Actor::getFirstName);

        assertEquals(List.of("PENELOPE"), page.list());
        assertEquals(42, page.totalCount());
        verify(query).setFirstRow(0);
    }

    @Test
    void allCapsResultsAtMaxPageSize() {
        List<Actor> all = repo.all();

        assertEquals(List.of(actor), all);
        verify(db).find(Actor.class);
        verify(query).setMaxRows(Repository.MAX_PAGE_SIZE);
    }

    @Test
    void allWithMapperMapsEveryEntity() {
        assertEquals(List.of("GUINESS"), repo.all(Actor::getLastName));
    }

    @Test
    void buildParserReturnsEbeanQueryParser() {
        assertInstanceOf(EbeanQueryParser.class, repo.buildParser("", Actor.class));
    }

    @Test
    void facetsDefaultToEmpty() {
        assertTrue(repo.facets("").isEmpty());
    }

    @Test
    void defaultLoadFieldsAreEmptyAndDbAccessorExposesDatabase() {
        assertTrue(repo.getLoadFields().isEmpty());
        assertSame(db, repo.db());
    }
}
