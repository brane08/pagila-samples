package com.github.brane08.pagila.actor.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.brane08.pagila.actor.beans.ActorInfo;
import com.github.brane08.pagila.actor.beans.ActorViewInfo;
import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.actor.entities.ActorView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ActorMapperTest {

    private final ActorMapper mapper = Mappers.getMapper(ActorMapper.class);

    private static Actor actor(Integer id, String first, String last) {
        Actor actor = new Actor();
        actor.setActorId(id);
        actor.setFirstName(first);
        actor.setLastName(last);
        actor.setLastUpdate(Instant.parse("2006-02-15T04:34:33Z"));
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

    @Test
    void actorToInfoMapsAllFields() {
        ActorInfo info = mapper.actorToInfo(actor(1, "PENELOPE", "GUINESS"));

        assertEquals(1, info.actorId());
        assertEquals("PENELOPE", info.firstName());
        assertEquals("GUINESS", info.lastName());
        assertEquals(Instant.parse("2006-02-15T04:34:33Z"), info.lastUpdate());
    }

    @Test
    void actorToInfoDefaultsNullIdToZero() {
        ActorInfo info = mapper.actorToInfo(actor(null, "NICK", "WAHLBERG"));

        assertEquals(0, info.actorId());
        assertEquals("NICK", info.firstName());
    }

    @Test
    void actorToInfoReturnsNullForNull() {
        assertNull(mapper.actorToInfo(null));
    }

    @Test
    void actorsToInfosMapsEachElementInOrder() {
        List<ActorInfo> infos = mapper.actorsToInfos(
                List.of(actor(1, "PENELOPE", "GUINESS"), actor(2, "NICK", "WAHLBERG")));

        assertEquals(2, infos.size());
        assertEquals("PENELOPE", infos.get(0).firstName());
        assertEquals(2, infos.get(1).actorId());
    }

    @Test
    void actorsToInfosReturnsEmptyListForNull() {
        assertTrue(mapper.actorsToInfos(null).isEmpty());
    }

    @Test
    void actorViewToInfoMapsAllFields() {
        ActorViewInfo info = mapper.actorViewToInfo(actorView(3, "ED", "CHASE", "ACADEMY DINOSAUR"));

        assertEquals(3L, info.getActorId());
        assertEquals("ED", info.getFirstName());
        assertEquals("CHASE", info.getLastName());
        assertEquals("ACADEMY DINOSAUR", info.getFilmInfo());
    }

    @Test
    void actorViewToInfoReturnsNullForNull() {
        assertNull(mapper.actorViewToInfo(null));
    }

    @Test
    void actorViewsToInfoMapsListAndHandlesNull() {
        List<ActorViewInfo> infos = mapper.actorViewsToInfo(
                List.of(actorView(3, "ED", "CHASE", "A"), actorView(4, "JENNIFER", "DAVIS", "B")));

        assertEquals(2, infos.size());
        assertEquals("JENNIFER", infos.get(1).getFirstName());
        assertTrue(mapper.actorViewsToInfo(null).isEmpty());
    }
}
