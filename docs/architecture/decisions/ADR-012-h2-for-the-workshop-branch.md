# ADR-012: H2 instead of PostgreSQL on the workshop branch

**Status:** Accepted  
**Date:** 2026-11-27  
**Requirements:** C-005, C-007, NFR-007  
**Affects:** Development View, Physical View  
**Amends:** [ADR-003](ADR-003-flyway-owns-the-schema.md)

## Context

The workshop runs in GitHub Codespaces, where participants have no Docker
daemon. On `main`, Testcontainers starts `postgres:17-alpine` for the
application (`spring-boot:test-run`), for every Spring test, and for jOOQ code
generation ([ADR-003](ADR-003-flyway-owns-the-schema.md)), so without Docker
nothing compiles. The workshop is about the specifications and the AI Unified
Process, not about the database, and a branch that builds with a JDK alone
(C-007) is worth more to it than PostgreSQL fidelity.

## Decision

**The `h2` branch runs on an embedded H2 database in PostgreSQL compatibility
mode** (C-005), everywhere: the application, both test layers, and code
generation.

- **Runtime and tests:** `spring.datasource.url` in `application.properties`
  points at an in-memory H2 database
  (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`). Flyway
  migrates it at startup; the test classpath adds the seed data as before, and
  the tests run against it with the same migrations (NFR-007).
- **Code generation:** in `generate-sources` the Flyway Maven plugin cleans and
  migrates a file-based H2 database under `target/`, and `jooq-codegen-maven`
  generates from it into the unchanged package
  `ai.unifiedprocess.demo.petclinic.database`. Its URL carries the same options
  as the runtime URL, so the generated names match the runtime schema.
- **Flyway still owns the schema** (C-006). The migrations change only where H2
  does not accept the PostgreSQL form:
  - `DEFAULT nextval(...)` comes before `PRIMARY KEY` in the column definitions.
  - H2 has no functional indexes, so `pets_owner_name_unique` is a unique
    constraint on `(owner_id, name_lower)`, where `name_lower` is a generated
    column `LOWER(name)`. The rule it backs (UC-007 BR-001) and the
    `DuplicateKeyException` the repository catches are unchanged.
  - The seed data uses `ON CONFLICT DO NOTHING` without a conflict target.

## Consequences

- **No Docker anywhere.** `./mvnw spring-boot:run`, `test`, `verify` and
  `generate-sources` need only a JDK (C-007); `spring-boot:test-run` starts the
  application with the seed data.
- **The database is gone on every restart.** In-memory is the point for a
  workshop: every start begins at the current migration.
- **DDL is not transactional in H2.** Tests that rename a table to provoke a
  data-access error (UC-002 A2, UC-010 A2) rename it back in a `finally` block
  instead of relying on the test transaction's rollback.
- **Dialect differences stay possible.** The SQL jOOQ renders is H2 SQL, and
  sorting compares strings by code point. A query that works here is not proven
  on PostgreSQL; `main` remains the PostgreSQL reference and this branch is not
  merged back.
- **The generated schema has one extra column**, `pets.name_lower`. It is
  written by the database only; no repository reads or writes it.
