# Stack

**For the agent:** read before writing code. This file is the AI Unified Process record of the technical layers,
independent of how the team documents its architecture. The Layers table says which layer skill builds and tests
each technical layer: `/use-case-implement` runs the Skill column bottom-up, `/use-case-test` runs the Test skill
column from the integration layer up, and `/use-case-coverage` searches for the markers in the last table.

**Last updated:** 2026-10-24

## Layers

| Layer       | Technology                | Skill              | Test skill              | Location                           |
|-------------|---------------------------|--------------------|-------------------------|------------------------------------|
| Migration   | Flyway                    | `flyway-schema`    | –                       | `src/main/resources/db/migration/` |
| Persistence | jOOQ                      | `jooq-persistence` | `jooq-persistence-test` | `<feature>/domain/`                |
| Service     | –                         | –                  | –                       | –                                  |
| API         | –                         | –                  | –                       | –                                  |
| UI          | Vaadin Flow               | `vaadin-flow-ui`   | `vaadin-flow-ui-test`   | `<feature>/ui/`                    |
| E2E         | Playwright + Drama Finder | –                  | `vaadin-e2e-test`       | `src/test/java/…/e2e/` (`*IT`)     |

There is no service layer (ADR-004): a Vaadin Flow view calls the feature's `<Entity>Repository` directly, the form
is the validation boundary (ADR-005), and the repository is the transaction boundary (ADR-009). This holds because
the Flow UI is the single entry point; there is no API layer (ADR-002). `vaadin-flow-ui` and `jooq-persistence`
follow that when the Service row is `–`.

**Integration layer:**

| Use case offered through | Tested in   | Test skill              |
|--------------------------|-------------|-------------------------|
| UI                       | UI          | `vaadin-flow-ui-test`   |
| Service                  | Persistence | `jooq-persistence-test` |

A layer the project does not have is marked `–` in every column (a UI layer in a REST service). The Integration
layer table has a row per entry layer of the project: a use case is tested in the first row its `**Layers:**` line
names, or in the first row when it has no such line. That layer's tests run in process against the real database
and exercise every layer below it, so `/use-case-test` runs its test skill and the ones above it and writes no tests
of their own for the layers below unless a run names them (`/use-case-test UC-001 persistence`). Without a service
layer, a use case offered through the service alone is tested at the repository.

**E2E scope:** Test cases only

`Use cases and test cases`: `/use-case-test` adds a browser test to every use case offered through the UI, and
`/test-case-implement` automates the TC-* journeys. `Test cases only`: browser tests are written for the journeys
alone, and a use case is tested in its integration layer. A project without an E2E layer drops the line. This
project decided `Test cases only` in ADR-006.

## Module layout

Single Maven module, package by feature under `ai.unifiedprocess.petclinic` (ADR-004). Each feature (`owner`, `pet`,
`visit`, `vet`, `welcome`) has exactly two sub-packages: `ui` (Vaadin views, forms, route parameter holders) and
`domain` (records and the jOOQ `<Entity>Repository`). `core/ui/` holds the application shell and error views.
Generated jOOQ sources live in `ai.unifiedprocess.demo.petclinic.database` under `target/generated-sources/jooq/`
and are never edited. Test seed data lives in `src/test/resources/db/migration/`. The full rules, including the
cross-feature routing exception, are in [development.md](development.md#module-layout).

## Commands

| Purpose            | Command                                                  |
|--------------------|----------------------------------------------------------|
| Build              | `./mvnw verify`                                          |
| Unit tests         | `./mvnw test` (sensors only: `./mvnw -q test -Dgroups=sensor`) |
| Integration tests  | `./mvnw test` (Browserless `UC<NNN><Name>Test`)          |
| End-to-end tests   | `./mvnw verify` (Playwright `TC<NNN><Name>IT`; one: `-Dit.test=<Class>`) |

## Markers

Each layer skill keeps its own rows here: on every run it checks that the rows for its layer match its marker
reference and corrects them. Do not edit the rows by hand; rerun the layer skill instead.

| Layer | Skill | Kind | Marker to grep for |
|-------|-------|------|--------------------|
| All   | –     | Code | `// UC-XXX BR-YYY:` directly above the method, query condition, or validator that enforces the rule |
| UI    | vaadin-flow-ui | Code | `UC-XXX` in the view's class Javadoc (`/** UC-XXX <Use Case Name>. */`)                                 |
| UI    | vaadin-flow-ui | Code | `// UC-XXX BR-YYY:` directly above the `Binder` validator or UI guard that enforces the rule            |
| UI    | vaadin-flow-ui | Name | `@Route` view class `<Name>View` named after the use case (`ManagePersonsView` for "Manage Persons")    |
| UI    | vaadin-flow-ui-test | Test | `@UseCase(id = "UC-XXX", scenario = "…", businessRules = {"BR-YYY"}, requirements = {"NFR-XXX"})` on every Browserless test method     |
| UI    | vaadin-flow-ui-test | Name | `UC<id><Name>Test`, e.g. `UC001ManagePersonsTest`, extending `SpringBrowserlessTest` or the project's subclass of it |
| E2E   | vaadin-e2e-test | Test | `@DisplayName("UC-XXX: …")` on the class and one `@Nested` `@DisplayName("A<n>: …")` per alternative flow |
| E2E   | vaadin-e2e-test | Test | `@TestCase(id = "TC-XXX", useCases = {…})` on the class, `@DisplayName("TC-XXX: …")`, and one `// Step <n>: <name>` comment per Flow row |
| E2E   | vaadin-e2e-test | Name | `UC<id><Name>IT` for a use case, `TC<id><Name>IT` for a test case journey                             |
