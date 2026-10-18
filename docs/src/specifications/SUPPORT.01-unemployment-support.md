# SUPPORT.01 · Unemployment social support

> **SUPPORT.01** · state **Confirmed** · traces from [SUPPORT-US-001](../user-stories/SUPPORT-US-001.md)
> · source `backend/src/main/java/ee/taltech/ite4120/support/`, `backend/src/main/resources/support/db/changelog/`, `frontend/src/pages/support/`

## Description

A case register for a local government's social workers: register an
unemployed resident's application for support, see three live registry checks
side by side, and approve or reject it. Create, search, view, update and
withdraw (soft delete) applications while they are open; a decision closes
them.

The three checks are asked over X-Road, each through its own adapter
(imitated by the application in this course, X-Road-shaped in production):

| Check | Registry | Passes when |
| --- | --- | --- |
| **Residence** | Population Register (Rahvastikuregister) | the person exists and their residence is in this municipality |
| **Unemployment** | Unemployment Insurance Fund (Töötukassa) | the person is registered as unemployed |
| **Employment** | Employment Register (töötamise register, EMTA) | the person has no active employment |

Each check ends as `PASS`, `FAIL` or `UNAVAILABLE` (the registry could not be
reached). The municipality the component serves is configuration
(`support.municipality`), not data.

**Privacy constraint — part of the specification.** Stored: the applicant's
personal code, the application, and the decision with the three check
outcomes. Never stored: names, addresses, dates or any other registry answer;
they are looked up live each time and shown only to the worker.

Excluded: payment, calculating the amount, appeals, a self-service portal for
applicants, authentication management (mock auth only), other support types.

## Data model

| Table | Field | Type | Rules |
| --- | --- | --- | --- |
| `support.support_type` | `code` PK | text | reference list: `ONE_OFF`, `MONTHLY` |
| | `name` | text | display name |
| `support.application` | `id` PK | bigint | from `core.seq_id` |
| | `applicant_isikukood` | text | required; exactly 11 digits — a string, never a number |
| | `support_type_code` | text | required; FK → support_type |
| | `period_start` | date | required |
| | `period_end` | date | required; not before `period_start` |
| | `household_size` | int | required; 1 … 20 |
| | `status` | text | `OPEN`, `APPROVED` or `REJECTED`; new applications are `OPEN` |
| | `sys_*` | — | platform columns, trigger-managed |
| `support.decision` | `id` PK | bigint | from `core.seq_id` |
| | `application_id` | bigint | required; FK → application; **unique** — one decision per application |
| | `outcome` | text | `APPROVED` or `REJECTED` |
| | `override` | boolean | required; true only for an approval with a check that did not pass |
| | `reason` | text | ≤2000; required when rejecting and when overriding |
| | `residence_check` | text | `PASS`, `FAIL` or `UNAVAILABLE`, as seen at the moment of deciding |
| | `unemployment_check` | text | same |
| | `employment_check` | text | same |
| | `sys_*` | — | platform columns; `sys_created_by` / `sys_created_at` are **who decided and when** |

Three tables, two foreign keys. Indexes on business tables carry
`where (sys_status = 'A')`. Support types are seeded with the reference
table; demo applications arrive under `context:demo`. Rules the database can
hold (formats, ranges, enumerations, `period_end ≥ period_start`, one decision
per application) are check constraints as well as service rules.

## API

| Method + path | Does | Failure answers |
| --- | --- | --- |
| `GET /api/support/applications` | paged search: `textContains` (personal code), `status`, `supportTypeCode`, `limit`, `offset`, `sort` (`periodStart`, `applicantIsikukood`) | 400 unknown sort key |
| `GET /api/support/applications/{id}` | one active application | 404 |
| `POST /api/support/applications` | register; **201 + Location** | 400 validation · 409 overlapping open application |
| `PUT /api/support/applications/{id}` | update an open application; the personal code is immutable | 400 · 404 · 409 already decided |
| `DELETE /api/support/applications/{id}` | withdraw an open application (soft) — **204** | 404 · 409 already decided |
| `GET /api/support/applications/{id}/checks` | the three checks, live; with the details the worker needs (name, address, municipality, unemployed since, employment active) — **never stored** | 404 |
| `POST /api/support/applications/{id}/decision` | decide: body `outcome`, `override`, `reason`. The server runs the three checks itself — it never trusts check results sent by the client — and stores their outcomes with the decision; **201** | 400 missing reason · 400 approval without override while a check did not pass · 404 · 409 already decided |
| `GET /api/support/applications/{id}/decision` | the decision of a decided application | 404 |
| `GET /api/support/types` | the reference list | — |

Every error is `application/problem+json`. All endpoints require
`Authorization: Bearer <user>` (mock auth); the contract is live at
`/swagger-ui.html`.

**Failure policy** — two different answers on purpose:
- **Viewing fails open.** If a registry is unreachable, `/checks` still
  answers 200 with that check `UNAVAILABLE`; the application and the other
  checks stay usable.
- **Approving fails closed.** `UNAVAILABLE` counts as not passing, so an
  approval then needs an override with a justification. Money must not be
  granted on a check nobody could make, unless a person takes responsibility
  for it in writing.

A person a registry does not know is a `FAIL`, not `UNAVAILABLE`.

**The registries.** Three adapters in `support/registry/`, each laid out like
`animals/ownerregistry/`: an interface, an HTTP adapter, an X-Road skeleton,
and one mapper from the wire record (Estonian field names, not translated)
to our domain record. In local mode the application imitates all three:

| Imitated endpoint | Wire record |
| --- | --- |
| `GET /mock-registry/population/{isikukood}` | `RrElukohtResponse(isikukood, eesnimi, perekonnanimi, aadress, omavalitsus)` |
| `GET /mock-registry/unemployment/{isikukood}` | `TkTootuResponse(isikukood, registreeritud, registreerimiseKuupaev)` |
| `GET /mock-registry/employment/{isikukood}` | `TorTootamineResponse(isikukood, aktiivneTootamine)` |

Each answers 404 for an unknown person. The mocks hold four **fictional**
people, enough to demonstrate every case: one who passes all three checks,
one who lives in another municipality, one with an active employment, and
one who is not registered as unemployed. Real people's data is not copied
into the mocks.

## UI

| Screen | Element | Component | Rules |
| --- | --- | --- | --- |
| Application list `/support` | table | `ResourceList` | columns: personal code (locked, **a link to the application**), support type tag (`AppTag`), period, household size, status tag; search field; filters for status and support type |
| | register button | `AppButtonPrimary` | navigates to the form |
| Application page `/support/{id}` | view / edit | `ResourceForm` + `useDataController` | section Application (personal code read-only, support type, period start and end, household size); Edit → Save is `PUT` and Withdraw is the soft `DELETE` — both only while `OPEN` |
| | checks | `AppCard` with one row per check, status as `AppTag` (green `PASS`, red `FAIL`, grey `UNAVAILABLE`) | loaded from `/checks`; shows name and address, unemployed since, and employment active; a registry being down shows that check as unavailable and nothing else changes |
| | decision | `AppCard`: `Radio.Group` Approve / Reject, `Checkbox` Override, `Input.TextArea` reason, `AppButtonPrimary` Decide | only while `OPEN`. Override is offered only when a check did not pass; reason is required for Reject and for an override. After deciding, the card shows the stored decision read-only: outcome, override, reason, the three check outcomes, who and when |
| | metadata | `ResourceForm.sidebar` — `AppCard` of `FieldItem`s | created at / by, modified at / by, version |
| Register form `/support/new` | personal code | `Input` | required, pattern `\d{11}` |
| | support type | `Select` | required, options from `/types` |
| | period | `DatePicker.RangePicker` | required; end not before start |
| | household size | `InputNumber` | required, 1 … 20 |
| | submit | `AppButtonPrimary` | 400/409 problem text shown verbatim in a notification |

## Business tests

1. **A personal code is exactly 11 digits.**
   Given a request with personal code `1234567890` (10 digits) or
   `3810213026A`, when it is registered, then it is refused (400) with
   "applicantIsikukood must be exactly 11 digits".
2. **A period does not end before it starts.**
   Given a period from 2026-11-01 to 2026-10-01, when it is registered, then it
   is refused (400) with "periodEnd must not be before periodStart".
3. **One open application per person for overlapping periods.**
   Given an open application for person X for October, when a second one for X
   covers 15 October to 15 November, then it conflicts (409). One for December
   is accepted. And when the October application is decided, an overlapping
   one may be registered again.
4. **A rejection needs a reason.**
   Given an open application, when it is rejected without a reason, then the
   decision is refused (400) with "reason is required to reject".
5. **All checks pass: approve directly; then it is closed.**
   Given registries that pass all three checks, when the application is
   approved without override, then the decision is stored with outcome
   `APPROVED`, override false and three `PASS` outcomes, and the application
   is `APPROVED`. And a later update, withdrawal or second decision conflicts
   (409).
6. **A failing check needs an override with a justification.**
   Given registries where the employment check fails, when the application is
   approved without override, then it is refused (400) with "a check did not
   pass: approve only as an override with a reason". And when approved with
   override and a reason, then it is stored with override true and
   `employment_check` `FAIL`.
7. **An unreachable registry is unavailable, not fatal.**
   Given the unemployment registry is down, when the checks are requested,
   then the answer is 200 with unemployment `UNAVAILABLE` and the other two
   checks present. And an approval then needs an override, as in test 6.

Tests 1–7 are backend tests (`SupportBusinessRulesIT`), with the three
registry adapters replaced by test doubles so each case is set up exactly.
The privacy constraint is verified by inspection: the decision table has no
columns for names, addresses or registry dates, and `/checks` reads live and
writes nothing.

**Open for the group to decide:** the final list of support types and
whether household size should change the rules (for example, a maximum
period per type).
