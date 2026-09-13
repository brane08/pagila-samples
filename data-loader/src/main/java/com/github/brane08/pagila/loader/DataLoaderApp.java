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
