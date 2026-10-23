# SUPPORT.01 · Unemployment social support

> **SUPPORT.01** · state **Proposed** (revision 2) · traces from [SUPPORT-US-001](../user-stories/SUPPORT-US-001.md)
> · source `backend/src/main/java/ee/taltech/ite4120/support/`, `backend/src/main/resources/support/db/changelog/`, `frontend/src/pages/support/`

## Description

A case register for a local government's social workers. A worker registers an
unemployed resident's application for a **municipal benefit scheme**, verifies
the facts that scheme's rules depend on from three state registries over
X-Road, resolves anything the registries could not confirm, and records a
**reasoned decision**.

**Revision 2** follows the group's legislation and policy analysis. Compared
with revision 1 it adds:
- benefit schemes tied to their legal instrument and rules;
- checksum validation of personal codes;
- an evidence record per check, with source and timestamps;
- the results `INCONCLUSIVE` and the status `PENDING_VERIFICATION`;
- manual verification;
- structured, legally reasoned rejections;
- an append-only audit log.

Revision 1's *override* becomes an **exception** that only applies to a
verified adverse fact.

The benefit is **assumed** to be a separate Tallinn municipal benefit for
unemployed residents, with cases handled by Mustamäe District Government. It is
not the statutory subsistence benefit (toimetulekutoetus). The legal instrument
and its rules are **placeholders until confirmed** (see *Outside the code*).

| Fact | Registry | Verified positive | Verified adverse | Inconclusive |
| --- | --- | --- | --- | --- |
| **Residence** | Population Register (Rahvastikuregister) | lives in `support.municipality` | lives in another municipality | person not found, or no municipality in the record |
| **Unemployment** | Unemployment Insurance Fund (Töötukassa) | registered as unemployed | not registered (or no record) | registered **and** an active employment: the registries contradict each other |
| **Employment** | Employment Register (töötamise register, EMTA) | no active employment (or no record) | an active employment | same contradiction as above |

A registry that cannot be reached gives `UNAVAILABLE`. Each check therefore
ends as `PASS` (verified positive), `FAIL` (verified adverse), `UNAVAILABLE`
or `INCONCLUSIVE`.

**Privacy constraint — part of the specification.**
- **Stored:** the personal code, the application, one minimal evidence record
  per check (result, a short status such as `REGISTERED_SINCE 2026-08-15`, the
  source, when it was retrieved, and the registry's own data timestamp), the
  decision, and audit events.
- **Never stored or logged:** names, addresses or full registry answers. They
  are shown to the worker only while verifying.

## Status flow

| Status | Means | Next |
| --- | --- | --- |
| `OPEN` | registered, not yet verified | verify → `PENDING_VERIFICATION` or `READY_FOR_DECISION` |
| `PENDING_VERIFICATION` | at least one **required** check is `UNAVAILABLE` or `INCONCLUSIVE` | verify again, or verify manually → `READY_FOR_DECISION` |
| `READY_FOR_DECISION` | every required check is `PASS` or `FAIL` | approve / reject |
| `APPROVED`, `REJECTED` | decided; final | — |

- **Undecided** means `OPEN`, `PENDING_VERIFICATION` or `READY_FOR_DECISION`.
- Editing an undecided application returns it to `OPEN`, because verification
  must be redone.
- Withdrawing (soft delete) is possible while undecided.
- A rejection that does not cite a registry fact (`INCOMPLETE_APPLICATION`,
  `OTHER`) is possible from any undecided status.

## Data model

Revision 1's changesets 01–03 and 90 are released and stay unchanged. Revision 2
adds changesets 04–08.

| Table | Field | Type | Rules |
| --- | --- | --- | --- |
| `support.support_type` — the **benefit scheme** | `code` PK, `name` | text | `ONE_OFF`, `MONTHLY` |
| | `legal_basis` | text | the governing instrument and provision — placeholder text until confirmed |
| | `rule_version` | text | e.g. `2026-1` |
| | `effective_from`, `effective_to` | date | `effective_to` optional; a scheme is offered only while effective |
| | `requires_residence`, `requires_unemployment`, `requires_no_employment` | boolean | which facts the scheme's rules depend on |
| | `review_info` | text | how a decision can be challenged; copied into every decision |
| `support.application` | (as revision 1) | | `applicant_isikukood`: valid Estonian personal code (format, birth date, checksum). `status` adds `PENDING_VERIFICATION` and `READY_FOR_DECISION` |
| `support.check_evidence` | `id` PK | bigint | from `core.seq_id` |
| | `application_id` | bigint | FK → application |
| | `check_type` | text | `RESIDENCE`, `UNEMPLOYMENT`, `EMPLOYMENT` |
| | `result` | text | `PASS`, `FAIL`, `UNAVAILABLE`, `INCONCLUSIVE` |
| | `status_detail` | text | ≤100; minimal returned status, e.g. `IN_MUNICIPALITY`, `REGISTERED_SINCE 2026-08-15`, `ACTIVE_EMPLOYMENT` — never names or addresses |
| | `source` | text | registry and service, e.g. `Töötukassa · tootuParing v1`, or `MANUAL · <source named by worker>` |
| | `retrieved_at` | timestamptz | when the system asked |
| | `source_data_at` | timestamptz | the registry's own "data as of" time; null if unknown or manual |
| | `manual` | boolean | true for a manual verification |
| | `note` | text | ≤1000; required for a manual verification |
| | `decision_id` | bigint | FK → decision; set on the evidence a decision relied on |
| | `sys_*` | — | platform columns; `sys_created_by` is who verified |
| `support.decision` | (as revision 1) | | adds `reason_code`, `legal_basis`, `rule_version`, `review_info`. `override` now means **exception**; `reason` is the **explanation** |
| `support.audit_event` | `id` PK | bigint | from `core.seq_id` |
| | `occurred_at` | timestamptz | default `now()` |
| | `actor` | text | the signed-in user |
| | `action` | text | `SEARCH`, `VIEW`, `CREATE`, `UPDATE`, `WITHDRAW`, `REGISTRY_QUERY`, `MANUAL_VERIFICATION`, `DECISION` |
| | `application_id` | bigint | when the action concerns one |
| | `detail` | text | ≤500; e.g. `Töötukassa: PASS` — **never** a registry payload, name or address |

`audit_event` is **append-only**: a trigger refuses `UPDATE` and `DELETE`. The
latest evidence row per application and check type is the current one; older
rows stay as history.

**Reason codes:**
- `NOT_RESIDENT`: needs residence `FAIL`
- `NOT_REGISTERED_UNEMPLOYED`: needs unemployment `FAIL`
- `ACTIVE_EMPLOYMENT`: needs employment `FAIL`
- `INCOMPLETE_APPLICATION` and `OTHER`: no registry fact needed

**Demo data.** Three demo personal codes from revision 1 fail the checksum. A
`context:demo` changeset replaces them with valid ones:

| Was | Now |
| --- | --- |
| 49002020022 | 49002020024 |
| 39003030033 | 39003030035 |
| 49004040044 | 49004040048 |

39001010011 is already valid and stays.

**Seeded schemes (placeholders):**

| Scheme | Requires |
| --- | --- |
| `ONE_OFF` | residence and unemployment |
| `MONTHLY` | all three facts |

## API

| Method + path | Does | Failure answers |
| --- | --- | --- |
| `GET /api/support/applications` | paged search: `textContains` (personal code), `status`, `supportTypeCode`, `limit`, `offset`, `sort`; a non-empty `textContains` writes a `SEARCH` audit event | 400 unknown sort key |
| `GET /api/support/applications/{id}` | one active application; writes `VIEW` | 404 |
| `POST /api/support/applications` | register; **201 + Location** | 400 validation · 409 overlapping undecided application (the message names it) |
| `PUT /api/support/applications/{id}` | update an undecided application; personal code immutable; status back to `OPEN` | 400 · 404 · 409 decided |
| `DELETE /api/support/applications/{id}` | withdraw an undecided application (soft) — **204** | 404 · 409 decided |
| `POST /api/support/applications/{id}/verification` | query the registries the scheme **requires**, store one evidence record per check, set the status. Answers the current evidence plus the live details (name, address) for display — **those are not stored** | 404 · 409 decided |
| `GET /api/support/applications/{id}/evidence` | the current evidence per check (stored data only) | 404 |
| `POST /api/support/applications/{id}/evidence/{checkType}/manual` | body `result` (`PASS`/`FAIL`), `source`, `note`: resolve an `UNAVAILABLE` or `INCONCLUSIVE` check from another authorised source; recomputes the status | 400 missing note or source · 404 · 409 the check is not unresolved, or the application is decided |
| `POST /api/support/applications/{id}/decision` | body `outcome`, `exception`, `reasonCode`, `explanation`. The server uses the stored evidence and copies `legal_basis`, `rule_version` and `review_info` from the scheme; **201** | 400 rule violations (below) · 404 · 409 decided |
| `GET /api/support/applications/{id}/decision` | the decision with its evidence | 404 |
| `GET /api/support/applications/{id}/audit` | the application's audit events | 404 |
| `GET /api/support/schemes` | effective benefit schemes with their rules | — |

**Decision rules** (400 when broken):
- **Approve:** only from `READY_FOR_DECISION`.
  - Every required check `PASS` → approve directly.
  - A required check `FAIL` → approve only with `exception: true` and an explanation.
- **Reject:** needs a `reasonCode` and an explanation. A registry reason code
  needs that check to be `FAIL`.
- **While `PENDING_VERIFICATION`:** no approval, and no rejection with a
  registry reason code.

Every error is `application/problem+json`. All endpoints require
`Authorization: Bearer <user>` (mock auth); the contract is live at
`/swagger-ui.html`.

**Failure policy:**
- **Viewing fails open:** an unreachable registry is one `UNAVAILABLE` check,
  and the other checks and the page still work.
- **Deciding fails safe:** `UNAVAILABLE` and `INCONCLUSIVE` neither approve
  nor reject anyone. They hold the application in `PENDING_VERIFICATION` until
  the fact is verified, by the registry or manually.

**The registries.** Three adapters in `support/registry/`:
- Each has an interface, an HTTP adapter, an X-Road skeleton and a mapper from
  Estonian wire records.
- Each wire record gains the registry's data timestamp `andmedSeisuga`.
- The system queries only the registries the scheme requires.
- The imitated registries at `/mock-registry/{population,unemployment,employment}`
  answer for the four fictional demo people.

## UI

| Screen | Element | Component | Rules |
| --- | --- | --- | --- |
| Application list `/support` | table, search, filters | `ResourceList` | as revision 1; status tags for all five statuses; "Benefit scheme" instead of "Support type" |
| Application page `/support/{id}` | application | `ResourceForm` + `useDataController` | Edit and Withdraw while undecided |
| | verification | `AppCard` with **Verify** (`AppButtonPrimary`) and one row per required check: result `AppTag` (green `PASS`, red `FAIL`, grey `UNAVAILABLE`, orange `INCONCLUSIVE`), status detail, source, retrieved at, data as of; live name and address after a verification, marked as not stored | a check not required by the scheme is shown as "not required" |
| | manual verification | per unresolved check: `Radio.Group` `PASS`/`FAIL`, `Input` source, `Input.TextArea` note, `AppButtonSecondary` | only for `UNAVAILABLE` / `INCONCLUSIVE` |
| | decision | `AppCard`: `Radio.Group` Approve / Reject, `Checkbox` Exception, `Select` reason code, `Input.TextArea` explanation, `AppButtonPrimary` Decide | Approve enabled only when `READY_FOR_DECISION`; Exception only when a required check is `FAIL`; after deciding, read-only with legal basis, rule version and review information |
| | audit | `AppCard` list of the application's audit events | read-only |
| Register form `/support/new` | as revision 1 | | personal code validated with checksum; scheme options from `/schemes` |

## Business tests

1. **A personal code is a valid Estonian personal code.**
   Given `1234567890`, `99001010011` (invalid first digit), `39013010011`
   (month 13) or `39001010012` (wrong checksum), when registered, then refused
   (400) with "applicantIsikukood is not a valid Estonian personal code"; and
   `39001010011` is accepted.
2. **A period does not end before it starts.** As in revision 1.
3. **One undecided application per person for overlapping periods.**
   Given an undecided application A for October, when another overlapping one
   is registered, then 409 naming application A. After A is decided, an
   overlapping one is accepted.
4. **Only the registries the scheme requires are asked, and evidence is
   recorded.** Given a `ONE_OFF` application (residence + unemployment), when
   it is verified, then the Employment Register is never called. Each stored
   evidence row has a result, source, retrieval time and data timestamp, and
   no names or addresses.
5. **Unverifiable is pending, not ineligible.** Given the unemployment
   registry is down, when a `MONTHLY` application is verified, then it is
   `PENDING_VERIFICATION`, approval is refused, and a rejection citing
   `NOT_REGISTERED_UNEMPLOYED` is refused. After a manual verification `PASS`
   with source and note, it is `READY_FOR_DECISION` and can be approved; the
   decision's evidence includes the manual record.
6. **Contradicting registries are inconclusive.** Given a person registered as
   unemployed **and** with an active employment, when verified, then both
   checks are `INCONCLUSIVE` and the application is `PENDING_VERIFICATION`.
7. **A verified adverse fact needs an exception.** Given an active employment
   (`FAIL`) for a `MONTHLY` application, approval without exception is refused
   (400). With `exception` and an explanation it is approved with exception
   true.
8. **A rejection is reasoned and carries the legal basis.** A rejection
   without reason code or explanation is refused. A rejection with
   `NOT_RESIDENT` is accepted when residence is `FAIL`, and stores the scheme's
   legal basis, rule version and review information.
9. **A decision is final.** After a decision, another decision, an update or a
   withdrawal conflicts (409).
10. **Searches, registry queries and decisions are audited without payloads.**
    A search by personal code, a verification and a decision each write audit
    events with the actor and application. No event contains a name or
    address. Updating or deleting an audit event is refused by the database.

Tests 1–10 are backend tests (`SupportBusinessRulesIT`), with the three
registry adapters as test doubles.

## Outside the code

These come from the legislation and policy analysis. They are organisational
or legal work, documented here so they are not mistaken for done:

- **Legal basis and eligibility matrix:** confirm the benefit, the Tallinn
  instrument and its rules; replace the placeholder `legal_basis` and rules.
- **Registry access:** authorisation from each data provider for this
  municipality, purpose and fields; a dedicated X-tee subsystem with
  service-level access rights.
- **Staff roles and reopening of decisions:** not built in this version (mock
  auth gives every user every right). Planned: caseworker roles that restrict
  searches and registry queries, and an authorised replacement-decision process
  with audit trail.
- **DPIA screening, privacy notice to applicants, retention schedule** for
  applications, evidence and audit events, and **monitoring and incident
  procedures**.
- Household size is recorded **for context only**; no rule uses it.

The source analysis cites GDPR (Art. 5, 6, 13, 15–16, 24–25, 32, 35), the
Social Welfare Act (§§ 14–15, 131–134), the Population Register Act (§§ 6, 65,
70), the Taxation Act (§ 25¹), the Administrative Procedure Act (§ 56), the
Public Information Act, and RIA's X-tee and E-ITS guidance.
