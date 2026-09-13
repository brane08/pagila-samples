package com.github.brane08.pagila.loader.slowquery;

import com.github.brane08.pagila.film.entities.Film;
import io.ebean.Database;

import java.util.List;

/**
 * film-to-actor is a lazy @ManyToMany. Accessing it per-row outside a fetch
 * triggers one extra round trip per row (N+1); {@code fetch("actors")}
 * collapses that back to a single query.
 */
public class NPlusOneFilmActorsDemo implements SlowQueryDemo {

    private static final int SAMPLE_SIZE = 20;

    @Override
    public String name() {
        return "nplusone";
    }

    @Override
    public void run(Database db) {
        System.out.println("=== N+1 demo: Film -> actors (lazy @ManyToMany) ===");

        long slowStart = System.nanoTime();
        // select("filmId") avoids reading film.rating — some sakila rows have
        // a blank rating_txt that Ebean's enum mapping can't parse, unrelated
        // to this demo's point
        List<Film> films = db.find(Film.class).select("filmId").setMaxRows(SAMPLE_SIZE).findList();
        int slowActorCount = 0;
        for (Film film : films) {
            // each call below is a separate SELECT against film_actor/actor —
            // one lazy-load round trip per film, on top of the initial film query
            slowActorCount += film.getActors().size();
        }
        long slowMs = (System.nanoTime() - slowStart) / 1_000_000;
        System.out.printf(
                "  slow (lazy):   %d films, %d actors, %d round trips (1 + %d), %dms%n",
                films.size(), slowActorCount, films.size() + 1, films.size(), slowMs);

        long fastStart = System.nanoTime();
        List<Film> fetched = db.find(Film.class).select("filmId").fetch("actors").setMaxRows(SAMPLE_SIZE).findList();
        int fastActorCount = 0;
        for (Film film : fetched) {
            fastActorCount += film.getActors().size(); // already loaded, no extra query
        }
        long fastMs = (System.nanoTime() - fastStart) / 1_000_000;
        System.out.printf(
                "  fast (fetch join): %d films, %d actors, 1 round trip, %dms%n",
                fetched.size(), fastActorCount, fastMs);
    }
}
