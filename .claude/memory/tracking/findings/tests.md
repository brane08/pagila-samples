# Test gaps (TS-nnn)

| ID | Gap | Status |
|---|---|---|
| TS-001 | quarkus-htmx changes not explicitly reviewed on JDK 25. JDK 25 build (enforcer on) and 24 REST contract tests vs live PG16 sakila already passed; confirm nothing else is needed. | FIXED 2026-10-26: Maven runs on Temurin 25.0.3, enforcer requires 25, no JDK override anywhere in poms |
