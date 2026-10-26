# Data Loader Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a small standalone Java module (`data-loader`) that can additively expand rows in the `sakila` DB for a given table, without touching existing data — so DB growth becomes an explicit, reproducible action instead of ad hoc drift.

**Architecture:** Plain Java CLI (`main()`), no Quarkus/Spring/CDI. Own HikariCP `DataSource` + plain `io.ebean.Database`, reusing `data-ebean`'s entity classes. A `TableLoader` interface with one implementation per table, registered in a name→loader map. `DataLoaderApp` parses `--table --count [--seed]` and dispatches.

**Tech Stack:** Java 25, Ebean 19.5.0 (`data-ebean` module), HikariCP (root `hikari-cp.version`), `net.datafaker:datafaker` for realistic values, postgresql driver.

**Spec:** inline below (no separate spec doc — small enough to fold into this plan)

## Global Constraints
- Additive only: never truncate/delete existing rows.
- Reuse `data-ebean` entities — do not redefine mappings.
- DB connection config via env vars (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`), same names as `genai-assistant/.env`; default to `localhost:5432/sakila`, user `postgres`, empty password — matching `quarkus-ebean/src/main/resources/application.yml`.
- Start with one table (`actor`) — design `TableLoader` so a second implementation is a single new class + one registry line, no interface changes.

---

### Task 1: Scaffold `data-loader` module + DB bootstrap

**Files:**
- Modify: `pom.xml` — add `<module>data-loader</module>`, add `<datafaker.version>2.4.4</datafaker.version>` property
- Create: `data-loader/pom.xml`
- Create: `data-loader/src/main/java/com/github/brane08/pagila/loader/DatabaseBootstrap.java`

**Interfaces:**
- Produces: `DatabaseBootstrap.open(): io.ebean.Database` — static method, reads env vars (falling back to the defaults above), builds a `HikaryDataSource`-backed `io.ebean.Database` with packages `com.github.brane08.pagila.actor.entities`, `.film.entities`, `.rental.entities`, `.seedworks.entities`, `.store.entities` (same list as `RepositoryProducers.ebeanDatabase`).

- [ ] **Step 1**: In root `pom.xml`, add `<module>data-loader</module>` next to the other modules, and add `<datafaker.version>2.4.4</datafaker.version>` next to `<mapstruct.version>`.

- [ ] **Step 2**: Create `data-loader/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.github.brane08.pagila</groupId>
    <artifactId>pagila-samples</artifactId>
    <version>1.0</version>
  </parent>
  <artifactId>data-loader</artifactId>

  <properties>
    <maven.compiler.release>${java.version}</maven.compiler.release>
  </properties>

  <dependencies>
    <dependency>
      <groupId>com.github.brane08.pagila</groupId>
      <artifactId>data-ebean</artifactId>
      <version>${project.version}</version>
    </dependency>
    <dependency>
      <groupId>com.zaxxer</groupId>
      <artifactId>HikariCP</artifactId>
      <version>${hikari-cp.version}</version>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>${postgresql.version}</version>
    </dependency>
    <dependency>
      <groupId>net.datafaker</groupId>
      <artifactId>datafaker</artifactId>
      <version>${datafaker.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>${maven-jar-plugin.version}</version>
        <configuration>
          <archive>
            <manifest>
              <mainClass>com.github.brane08.pagila.loader.DataLoaderApp</mainClass>
            </manifest>
          </archive>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>${maven-dependency-plugin.version}</version>
        <executions>
          <execution>
            <id>copy-deps</id>
            <phase>package</phase>
            <goals><goal>copy-dependencies</goal></goals>
            <configuration>
              <outputDirectory>${project.build.directory}/lib</outputDirectory>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3**: Create `data-loader/src/main/java/com/github/brane08/pagila/loader/DatabaseBootstrap.java`:
```java
package com.github.brane08.pagila.loader;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.annotation.Platform;
import io.ebean.config.DatabaseConfig;

import java.util.List;

public final class DatabaseBootstrap {

    private DatabaseBootstrap() {
    }

    public static Database open() {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "5432");
        String name = env("DB_NAME", "sakila");
        String user = env("DB_USER", "postgres");
        String password = env("DB_PASSWORD", "");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + name);
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(4);

        DatabaseConfig config = new DatabaseConfig();
        config.setDataSource(new HikariDataSource(hikariConfig));
        config.setDatabasePlatformName(Platform.POSTGRES.name());
        config.setPackages(List.of(
                "com.github.brane08.pagila.actor.entities",
                "com.github.brane08.pagila.film.entities",
                "com.github.brane08.pagila.rental.entities",
                "com.github.brane08.pagila.seedworks.entities",
                "com.github.brane08.pagila.store.entities"));
        return DatabaseFactory.create(config);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
```

- [ ] **Step 4**: Verify it compiles: `mvn -q -pl data-loader -am compile`. Expected: no output, exit 0.

- [ ] **Step 5**: Commit
```bash
git add pom.xml data-loader/pom.xml data-loader/src/main/java/com/github/brane08/pagila/loader/DatabaseBootstrap.java
git commit -m "Scaffold data-loader module with plain Ebean DB bootstrap"
```

---

### Task 2: `TableLoader` interface + `ActorLoader` + CLI entrypoint

**Files:**
- Create: `data-loader/src/main/java/com/github/brane08/pagila/loader/TableLoader.java`
- Create: `data-loader/src/main/java/com/github/brane08/pagila/loader/loaders/ActorLoader.java`
- Create: `data-loader/src/main/java/com/github/brane08/pagila/loader/DataLoaderApp.java`

**Interfaces:**
- Consumes: `DatabaseBootstrap.open(): Database` (Task 1)
- Produces: `TableLoader.tableName(): String`, `TableLoader.load(Database db, int count, long seed): int` (returns rows inserted) — later table loaders implement this same interface.

- [ ] **Step 1**: Create `TableLoader.java`:
```java
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
```

- [ ] **Step 2**: Create `loaders/ActorLoader.java`:
```java
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
```

- [ ] **Step 3**: Create `DataLoaderApp.java`:
```java
package com.github.brane08.pagila.loader;

import com.github.brane08.pagila.loader.loaders.ActorLoader;
import io.ebean.Database;

import java.util.Map;

public class DataLoaderApp {

    private static final Map<String, TableLoader> LOADERS = Map.of(
            "actor", new ActorLoader()
    );

    public static void main(String[] args) {
        String table = argValue(args, "--table");
        String countArg = argValue(args, "--count");
        String seedArg = argValue(args, "--seed");

        if (table == null || countArg == null) {
            System.err.println("Usage: --table <name> --count <n> [--seed <n>]");
            System.err.println("Known tables: " + LOADERS.keySet());
            System.exit(1);
        }

        TableLoader loader = LOADERS.get(table);
        if (loader == null) {
            System.err.println("Unknown table '" + table + "'. Known tables: " + LOADERS.keySet());
            System.exit(1);
        }

        int count = Integer.parseInt(countArg);
        long seed = seedArg != null ? Long.parseLong(seedArg) : System.nanoTime();

        Database db = DatabaseBootstrap.open();
        int inserted = loader.load(db, count, seed);
        System.out.println("Inserted " + inserted + " rows into '" + table + "' (seed=" + seed + ")");
    }

    private static String argValue(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        return null;
    }
}
```

- [ ] **Step 4**: Build the runnable jar: `mvn -q -pl data-loader -am package -DskipTests`. Expected: exit 0, `data-loader/target/data-loader-1.0.jar` and `data-loader/target/lib/*.jar` exist.

- [ ] **Step 5**: Verify additive load against the live `sakila` DB. Record the before-count, run the loader, record the after-count:
```bash
bash ~/.claude/skills/psql-select/select.sh -d sakila -c "SELECT count(*) FROM actor;"
java -cp "data-loader/target/data-loader-1.0.jar:data-loader/target/lib/*" com.github.brane08.pagila.loader.DataLoaderApp --table actor --count 50 --seed 42
bash ~/.claude/skills/psql-select/select.sh -d sakila -c "SELECT count(*) FROM actor;"
```
Expected: second count = first count + 50; program prints `Inserted 50 rows into 'actor' (seed=42)`.

- [ ] **Step 6**: Commit
```bash
git add data-loader/src/main/java/com/github/brane08/pagila/loader
git commit -m "Add TableLoader interface, ActorLoader, and data-loader CLI entrypoint"
```

---

## Self-Review

- **Spec coverage:** additive-only ✅ (Task 2 Step 2, no delete/update); reuses `data-ebean` entities ✅ (Task 2 imports `Actor` from `data-ebean`); env-var config with stated defaults ✅ (Task 1 Step 3); starts with one table, extensible via registry ✅ (`TableLoader` + `LOADERS` map — a second table is one new class + one map entry).
- **Placeholder scan:** none — all steps have literal code/commands.
- **Type consistency:** `TableLoader.load(Database, int, long): int` used identically in `ActorLoader` and `DataLoaderApp`; `DatabaseBootstrap.open(): Database` used identically in `DataLoaderApp`.
