package com.github.brane08.pagila.loader.slowquery;

import io.ebean.Database;
import io.ebean.SqlRow;

import java.util.List;

/**
 * Compares a filter with no supporting index (LIKE on film.description,
 * forcing a Seq Scan) against a filter on the primary key (Index Scan),
 * using EXPLAIN ANALYZE so the plan difference is the actual proof, not
 * just a timing guess.
 */
public class UnindexedFilmSearchDemo implements SlowQueryDemo {

    @Override
    public String name() {
        return "unindexed";
    }

    @Override
    public void run(Database db) {
        System.out.println("=== Unindexed filter demo: film.description LIKE vs film_id PK lookup ===");

        System.out.println("  slow (no index on description, Seq Scan):");
        printPlan(db, "EXPLAIN ANALYZE SELECT * FROM film WHERE description LIKE '%love%'");

        System.out.println("  fast (film_id primary key, Index Scan):");
        printPlan(db, "EXPLAIN ANALYZE SELECT * FROM film WHERE film_id = 1");
    }

    private void printPlan(Database db, String sql) {
        List<SqlRow> rows = db.sqlQuery(sql).findList();
        for (SqlRow row : rows) {
            System.out.println("    " + row.getString("QUERY PLAN"));
        }
    }
}
