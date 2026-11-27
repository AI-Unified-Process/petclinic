# Physical View

**Question:** Where does the system run?  
**Artifacts:** this document (deployment diagram, environments, constraints)  
**Notation:** Mermaid deployment diagram, Markdown  
**For the agent:** the boundary conditions. An agent that knows the database is
an embedded H2 (on this workshop branch, ADR-012), that the session state lives in the server, and that the app runs on
a trusted network inside the clinic decides differently from one that does not.

## Deployment

```mermaid
flowchart LR
    subgraph clinic["Clinic network (trusted, no public access)"]
        B["Browser<br/>front desk workstation"]
        subgraph host["Application host"]
            APP["aiup-petclinic.jar<br/>Spring Boot · embedded Tomcat :8080<br/>Vaadin UI state in the HTTP session"]
            DB[("H2, in memory, PostgreSQL mode<br/>schema owned by Flyway")]
        end
    end
    B <-->|" HTTP · Vaadin Flow protocol "| APP
    APP -->|JDBC, in process| DB
```

One node, one process: the database is embedded in it. No reverse proxy, no
cache, no message broker, no second service — the Logical and Process views have nothing that would need one.

## Environments

| Environment      | How it starts                                                            | Database                                                                   | Notes                                                                                                           |
|------------------|--------------------------------------------------------------------------|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| **Local dev**    | `./mvnw spring-boot:run` (empty) · `./mvnw spring-boot:test-run` (`TestPetclinicApplication`, seeded) | in-memory H2, emptied on every restart                     | No Docker (C-007). Spring DevTools and Vaadin dev tools are active                                              |
| **Build / test** | `./mvnw test` · `./mvnw verify`                                          | in-memory H2 per test JVM; jOOQ code generation migrates a file-based H2 under `target/` | No Docker (C-007)                                                                                               |
| **CI**           | GitHub Actions, `ubuntu-latest`, JDK 25, `mvn -B verify` + Sonar scanner | in-memory H2, as in Build / test                                           | `.github/workflows/build.yml`; caches `~/.m2` and the Playwright browsers                                       |
| **Production**   | `java -jar aiup-petclinic.jar` (fat JAR from `./mvnw package`)           | none on this branch — `main` targets an external PostgreSQL 17             | Target state — **nothing is deployed today**; see *Not there yet*                                               |

Flyway runs at startup and brings the schema to the current version. The
test-only `V2__seed_reference_data.sql` lives on the test classpath and never
reaches a production database.

## Runtime constraints

- **Server-side UI state means sticky sessions.** Vaadin holds the component
  tree of every open UI in the HTTP session. One instance is assumed. Two
  instances behind a load balancer require session affinity — session
  replication is not configured, and a session lost on failover means the user's
  open form is gone. Scale vertically first; a second instance is an
  architecture change, not a configuration change.
- **One database, no read replicas.** Every query goes to the primary.
- **H2 only on this branch** (C-005, ADR-012). The migrations are PostgreSQL
  SQL that H2 accepts in PostgreSQL mode, and the code is generated from the H2
  schema. A query that works here is not proven on PostgreSQL; `main` is the
  PostgreSQL reference.
- **Port 8080** by default. Nothing but the Vaadin UI is served: Spring Boot
  Actuator is deliberately not on the classpath, because no requirement asks
  for a health or metrics endpoint and an unsecured one would be the first thing
  to lock down.

## Security and data

- **No authentication, no authorization** (C-010). Whoever reaches the
  application can do everything a Clinic User can, including reading and
  changing customer data. This is deliberate for the demo and is the single
  precondition of the whole deployment: **the application may not be exposed
  beyond the clinic's own network.** Adding authentication is the first change a
  real deployment needs, and it changes the Use Case View too (the Visitor /
  Clinic User split becomes enforced instead of implied).
- **TLS is not terminated by the application.** If it is needed, it belongs in
  front of the app.
- **Owner records are personal data** — name, address, telephone. No backup
  schedule, retention period, or hosting location is specified anywhere in
  `docs/`, and none can be invented here. For a real clinic these have to be
  supplied before deployment.

## Not there yet

Honest list of what a production deployment would add. None of it exists in the
repository today, so an agent should not pretend to change it:

- no container image (`./mvnw spring-boot:build-image` would produce one),
- no infrastructure as code, no deployment pipeline beyond the CI build,
- no externalized production configuration (`spring.datasource.*` points at
  the in-memory H2 in every environment that exists today),
- no monitoring, log aggregation, or alerting,
- no backup or restore procedure,
- no availability target, and no measured response-time budget to hold the
  "keeps up with a conversation" goal from the vision to.
