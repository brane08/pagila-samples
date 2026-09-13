package com.github.brane08.pagila.loader;

import com.github.brane08.pagila.loader.slowquery.NPlusOneFilmActorsDemo;
import com.github.brane08.pagila.loader.slowquery.SlowQueryDemo;
import com.github.brane08.pagila.loader.slowquery.UnindexedFilmSearchDemo;
import io.ebean.Database;

import java.util.LinkedHashMap;
import java.util.Map;

public class SlowQueryApp {

    private static final Map<String, SlowQueryDemo> DEMOS = new LinkedHashMap<>();

    static {
        register(new NPlusOneFilmActorsDemo());
        register(new UnindexedFilmSearchDemo());
    }

    private static void register(SlowQueryDemo demo) {
        DEMOS.put(demo.name(), demo);
    }

    public static void main(String[] args) {
        String demoArg = argValue(args, "--demo");

        if (demoArg == null) {
            System.err.println("Usage: --demo <name>|all");
            System.err.println("Known demos: " + DEMOS.keySet());
            System.exit(1);
        }

        Database db = DatabaseBootstrap.open();

        if (demoArg.equals("all")) {
            for (SlowQueryDemo demo : DEMOS.values()) {
                demo.run(db);
            }
            return;
        }

        SlowQueryDemo demo = DEMOS.get(demoArg);
        if (demo == null) {
            System.err.println("Unknown demo '" + demoArg + "'. Known demos: " + DEMOS.keySet());
            System.exit(1);
        }
        demo.run(db);
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
