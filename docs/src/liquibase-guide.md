# Liquibase guide — the support changelog, changeset by changeset

The `support` schema is built entirely by Liquibase from
`backend/src/main/resources/support/db/changelog/`. Nothing is ever typed into
`psql` by hand: the changelog **is** the schema's biography, and every
environment — your laptop, a teammate's, CI, the defence demo — converges by
replaying the same steps in the same order.

## Anatomy of a changeset

```sql
--liquibase formatted sql

--changeset ite4120:support-02-application
--comment The application for unemployment support
/* Prose about the changeset lives in a block comment like this one. */
create table support.application ( ... );
--
```

`author:id` (`ite4120:support-02-application`) is the changeset's identity. Liquibase
records it in `public.databasechangelog` together with a **checksum** of the SQL.
On every start it compares: not yet recorded → run it; recorded and unchanged →
skip it; recorded but **edited** → refuse to start.

## The grammar: `--` belongs to Liquibase

In formatted SQL, `--` is **not a comment marker** — it is the escape character
for Liquibase's own grammar. `--changeset` opens a unit, `--rollback` inside a
body silently becomes rollback SQL, and a wrapped prose line whose continuation
happens to begin with `changeset`, `comment`, `rollback` or another directive
word is parsed as that directive. So:

- `--` lines belong to Liquibase: the preamble, the directives, and the
  terminator. Nothing else.
- Prose is a single-line `--comment` followed by a `/* */` block. Inside a
  changeset body, every comment that *starts* a line is a `/* */` block
  (a trailing `-- …` after SQL is fine — it does not start the line).
- Every changeset — including a file's last — closes with a bare `--` line,
  the terminator. Without it, anything later appended below silently joins
  this changeset's checksum body.
- Datatypes align at column 25: a four-space indent, then the column name
  left-padded to 24 characters total. `constraint` lines keep a single space.
- Indexes on business tables carry `where (sys_status = 'A')` — retired rows
  do not need indexing.

These are the platform's production rules (`emr/docs/architecture/data/02-migration.md`),
mechanically enforced there; here they are taught by example — every file in
this changelog follows them.

## The one rule: released changesets are immutable

That checksum is why **you never edit a changeset that has run anywhere** — not
on a teammate's machine, not in CI. You append a new one. The support changelog
shows this several times:

| File | What it does |
| --- | --- |
| `support-schema.sql` | `create schema` — one schema per component |
| `01-support-type.sql` | Reference table: natural key, no sys columns — DDL and its seed rows as two separate changesets |
| `02-application.sql` | Business table: `core.seq_id` ids, `sys_*` columns, `create_table_metadata()` trigger install, partial indexes on active rows |
| `03-decision.sql` | Child table with FK to `application` — with 01 and 02, three tables and two FKs |
| `04-benefit-scheme.sql` | **Evolution.** Revision 2 turned support types into benefit schemes (legal basis, rule version, which checks apply). We did not edit 01 — we appended an ALTER |
| `05-application-status.sql` | Widens the status check for the revision 2 statuses — drop and re-add the constraint |
| `06-check-evidence.sql` | One evidence record per registry check, FKs to `application` and `decision` |
| `07-decision-reasons.sql` | Reason codes for decisions; backfills older rejections before the new constraint is added |
| `08-audit-event.sql` | Append-only audit log: a PL/pgSQL trigger, so the changeset uses `splitStatements:false` |
| `09-drop-animal-example.sql` | Drops the course template's animal example schema from any database that still has it |
| `90-demo-data.sql`, `91-demo-valid-codes.sql` | Seed rows behind `context:demo`; 91 corrects demo codes that failed the checksum instead of editing 90 |

## The core includes

The master changelog's first include is not ours:

```xml
<include file="core-db/changelog/changelog.xml"/>
```

That path resolves **inside the published `org.helex.emr:commons-db-core` jar**.
It creates the shared `core` schema: the id sequence (`core.seq_id`), the
sys-column trigger machinery and the session functions our DDL calls. This is the
platform pattern: shared schema ships as a library, modules include it.

## Reset vs rollback

In development you do not roll back — you **reset**: `scripts/reset-db` destroys
the database and the next backend start replays the whole changelog from empty.
The Testcontainers tests do the same on every run, which is why a changelog that
only works "from where my database happens to be" fails CI immediately.

## Deliberate deviations from the platform rules

Two production rules are relaxed here, knowingly:

- **One database user.** Production separates a DDL-privileged Liquibase user
  from a minimal runtime user (created by a `prod`-context changeset). The
  application runs both as `support` — one less credential to fumble in a lab.
- **Demo data lives in-tree.** Production ships demo data as separate
  `helex-demo-*` artifacts discovered through an extension point; the product
  tree names no customers. This repository keeps `90-demo-data.sql` in-tree —
  the transferable lesson is the `context:demo` gate, not the packaging.

## Adding a component

Copy the shape exactly: `<component>/db/changelog/<component>/` with a schema
changeset, numbered table changesets, and one include added to the master
changelog (`backend/src/main/resources/db/changelog.xml`). If you need to change
a table you already merged — that is an appended changeset.
