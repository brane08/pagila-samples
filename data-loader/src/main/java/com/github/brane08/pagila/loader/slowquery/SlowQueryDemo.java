package com.github.brane08.pagila.loader.slowquery;

import io.ebean.Database;

public interface SlowQueryDemo {
    String name();

    void run(Database db);
}
