# Manual · Database

The data layer in one page: what is in the database, where it comes from, and
how you change it. The long form, changeset by changeset, is the
[Liquibase guide](../liquibase-guide.md); the data model itself is in the
[specification](../specifications/SUPPORT.01-unemployment-support.md#data-model).

## What runs where

| Thing | Where |
| --- | --- |
| PostgreSQL 18 | Docker, `localhost:18520`, database and user `support` — `docker/compose.yaml` |
| The schema | created by **Liquibase on backend start** — nothing is created by hand or by init scripts |
| Master changelog | `backend/src/main/resources/db/changelog.xml` |
| Component changelog | `…/support/db/changelog/support/` — numbered `.sql` files, in order |
| Liquibase's own bookkeeping | schema `public`: `databasechangelog`, `databasechangeloglock` |

The master changelog includes two things, in this order: the platform's `core`
schema, which ships **inside the published `commons-db-core` jar**, and the
support component's changelog. Another component would be a third include.

## Two schemas

- **`core`** — the platform's, not yours. It provides `core.seq_id` (one id
  sequence for the whole database), the `core.sys_columns()` trigger that fills
  the `sys_*` columns, and `core.create_table_metadata()` that installs it.
- **`support`** — unemployment social support: `support_type` (the benefit
  schemes, a reference list), `application` (the business table), `decision`,
  `check_evidence` and `audit_event`. Five tables and four foreign keys — above
  the assessment's data-layer minimum. One schema per component: boundaries in
  the database mirror boundaries in the code.

## Required minimum

What the assessment's data-layer gate (gate A) checks, and where this component
meets it:

| Requirement | In this component |
| --- | --- |
| **At least three tables, at least one foreign key**, in a schema of your own | `support_type`, `application`, `decision`, `check_evidence`, `audit_event` — FKs `application → support_type`, `decision → application`, `check_evidence → application`, `check_evidence → decision` |
| Every business table: `id` from `core.seq_id`, the `sys_*` columns, `core.create_table_metadata()` called | `02-application.sql`, `03-decision.sql`, `06-check-evidence.sql` |
| Soft delete via `sys_status`; uniqueness enforced among active rows only | `decision_application_ukey … where (sys_status = 'A')` — one decision per application |
| Identifiers stored as `text`, never as numbers | `applicant_isikukood` |
| Only Liquibase changesets create or change anything; released ones are never edited | `changelog.xml` + numbered files; `04-…` to `09-…` append |
| Demo rows under `context:demo`, so the app is usable out of the box | `90-demo-data.sql`, `91-demo-valid-codes.sql` |
| `scripts/check-changesets` clean, `./gradlew test` green from an empty database | — |

A reference table (like `support_type`) counts as one of the three; data owned
by an external registry (a person's name and address) is looked up, never
stored — only the check result is kept as evidence —
see the [REST API manual](rest-api.md#the-registry-adapter).

## Anatomy of a business table

Read `02-application.sql` with this list beside it:

| Line | Why |
| --- | --- |
| `id bigint default nextval('core.seq_id') not null` | ids come from the shared sequence, never `serial` |
| business columns | identifiers (`applicant_isikukood`, `support_type_code`) are `text` — **strings, never numbers** |
| `sys_status, sys_version, sys_created_at, sys_created_by, sys_modified_at, sys_modified_by` | declared here, **filled by the trigger** — application code never writes them |
| `select core.create_table_metadata('support.application');` | installs that trigger; one call per business table |
| `create index … where (sys_status = 'A')` | indexes cover **active** rows only; a unique index this way (as `decision_application_ukey`) enforces uniqueness among active rows |
| `check (applicant_isikukood ~ '^[0-9]{11}$')` | the database is the last line of validation |

**Soft delete**: rows are never deleted; withdrawing an application sets
`sys_status` from `A` to `C`. Every query in the repository filters `sys_status = 'A'`.

**Reference tables** (`01-support-type.sql`) are different on purpose: natural
key, no `sys_*` columns, and a separate seed changeset.

**The audit log** (`08-audit-event.sql`) is append-only: a trigger refuses
`update` and `delete`, so the history of who searched, viewed, verified and
decided cannot be rewritten.

**Who am I?** The trigger stamps `sys_created_by` / `sys_modified_by` from
`core.session_user()`, which reads the session setting `core.client_identifier`.
Two things set it: the pool, once per connection, to the application's own name
(`connection-init-sql` in `application.yml` — what Liquibase and demo data are
attributed to), and `JdbcConfig`, on every connection a request borrows, to the
signed-in user via `core.set_user(?)` — the platform's `JdbcFactory` pattern.
So a row records who created it and who last changed it, and an update that
changes nothing leaves the row untouched (the trigger compares `new = old`).

## Changesets: the two rules that bite

1. **A released changeset is immutable.** Liquibase stores a checksum of every
   changeset it ran and refuses to start on an edited one. To change a table
   you already merged, append a new file — `04-benefit-scheme.sql` is the
   example.
2. **Formatted-SQL grammar.** In `--liquibase formatted sql` files a `--` line
   is a Liquibase directive, not a comment: `--changeset`, `--comment`, and
   the bare `--` that terminates every changeset. Prose goes in `/* */`
   blocks. Datatypes align at column 25. The gate is
   `scripts/check-changesets` — run it before every PR that touches a
   changelog; it fails closed.

Demo rows live in `90-demo-data.sql` and `91-demo-valid-codes.sql` under
`context:demo` and are applied
only because `application.yml` sets `spring.liquibase.contexts: demo`.

## Working with the database

```bash
./scripts/run-backend.sh     # starts PostgreSQL and applies the changelog
./scripts/psql.sh            # psql inside the container
./scripts/reset-db.sh        # destroy and recreate — the database is disposable
```

Useful in `psql`:

```sql
\dt support.*                                            -- the component's tables
\d support.application                                   -- columns, constraints, indexes, trigger
select id, filename, orderexecuted from public.databasechangelog order by orderexecuted;
select applicant_isikukood, status, sys_status, sys_created_by from support.application;
```

When the backend refuses to start after a library bump complaining about
checksums of `core-db` changesets, that is rule 1 doing its job: reset the
database, it replays clean.

## Tests

`./gradlew test` starts a throwaway PostgreSQL via Testcontainers and applies
the **whole** changelog from empty before the business tests run — so every
test run also proves the migrations. `application-test.yml` only redirects the
core changelog's grant parameters at the container's user; the component
needs nothing there.

## Adding a component

1. `backend/src/main/resources/<component>/db/changelog/<component>/` with
   `<component>-schema.sql`, then `01-…sql`, `02-…sql` per table, then optionally
   `90-demo-data.sql` with `context:demo`.
2. A `changelog.xml` beside them listing the files in order, and **one**
   `<include>` for it in the master changelog.
3. `scripts/check-changesets`, then `./gradlew test` — green means the
   migrations run from empty.

References: [Liquibase guide](../liquibase-guide.md) · the
[data model](../specifications/SUPPORT.01-unemployment-support.md#data-model) ·
`AGENTS.md` rules 5–6 · the files under
`backend/src/main/resources/support/db/changelog/support/`.
