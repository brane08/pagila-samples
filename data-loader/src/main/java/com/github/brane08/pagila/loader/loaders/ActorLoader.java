package com.github.brane08.pagila.loader.loaders;

import com.github.brane08.pagila.actor.entities.Actor;
import com.github.brane08.pagila.loader.TableLoader;
import io.ebean.Database;
import net.datafaker.Faker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ActorLoader implements TableLoader {

    private static final int BATCH_SIZE = 200;

    @Override
    public String tableName() {
        return "actor";
    }

    @Override
    public int load(Database db, int count, long seed) {
        Faker faker = new Faker(new Random(seed));
        List<Actor> batch = new ArrayList<>(BATCH_SIZE);
        int inserted = 0;

        for (int i = 0; i < count; i++) {
            Actor actor = new Actor();
            actor.setFirstName(faker.name().firstName());
            actor.setLastName(faker.name().lastName());
            batch.add(actor);

            if (batch.size() == BATCH_SIZE || i == count - 1) {
                db.saveAll(batch);
                inserted += batch.size();
                batch.clear();
            }
        }
        return inserted;
    }
}
