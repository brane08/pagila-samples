package com.github.brane08.pagila.loader;

import io.ebean.Database;

public interface TableLoader {
    String tableName();

    /**
     * Inserts {@code count} new rows; never deletes or updates existing ones.
     * @return number of rows inserted
     */
    int load(Database db, int count, long seed);
}
