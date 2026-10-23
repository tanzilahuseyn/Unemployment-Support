# SUPPORT-US-001 · Decide unemployment support from live registry data

> **SUPPORT-US-001** · state **Proposed** (revision 2) · traces to [SUPPORT.01](../specifications/SUPPORT.01-unemployment-support.md)

**As** a social worker in a local government
**I want** to register an unemployed resident's application for a municipal
support benefit, verify the facts the benefit's rules depend on from the state
registries, and record a reasoned decision
**so that** I can decide lawfully, fairly and quickly, without asking the
applicant for paper certificates the state already holds.

## Context

The benefit is a **municipal benefit for unemployed residents**, assumed to be
administered by Tallinn with the case handled in Mustamäe District. It is
**not** the statutory subsistence benefit (toimetulekutoetus), which is decided
from household income and housing costs. Each benefit scheme is defined by its
own legal instrument, and only that instrument says which facts matter.

The facts the schemes can depend on live in three state registries:

- **residence** in the municipality: the Population Register (Rahvastikuregister);
- **registration as unemployed**: the Unemployment Insurance Fund (Töötukassa);
- **no active employment**: the Employment Register (töötamise register) of the
  Tax and Customs Board.

Estonia's "once only" principle says the state should not ask a person for data
it already has. The component asks the registries over X-Road (imitated in this
course) and shows the answers side by side.

Registries can be unreachable, out of date or inconsistent. **Not being able to
verify a fact is not proof that the person is ineligible.** Such an application
stays pending verification until the fact is verified, by the registry or
manually by the worker from another authorised source. A worker can also
approve as a recorded **exception** where the scheme's rules allow individual
assessment.

**Privacy by design.** Only what the decision needs is stored: the personal
code, the application, a minimal evidence snapshot per check (result, source,
when it was retrieved, and how current the registry's data was) and the
reasoned decision. Names, addresses and full registry answers are looked up
live and never stored or logged. Registry queries, searches and decisions are
written to an audit log.

This revision follows the group's legislation and policy analysis (GDPR, the
Social Welfare Act, the Population Register Act, the Taxation Act, the
Administrative Procedure Act § 56, and X-tee guidance). See SUPPORT.01 for
what is built and what is organisational work outside the code.

Out of scope: paying the support, calculating amounts, the statutory
subsistence benefit, the applicant's self-service portal, and staff roles and
reopening of decisions (planned, not in this version).

## Acceptance criteria

1. The worker registers an application with the applicant's personal code, the
   **benefit scheme**, the application period and the household size. Each
   scheme names its legal instrument, rule version, effective dates and which
   registry facts its rules require.
2. The personal code is validated against the official Estonian format: 11
   digits, a valid first digit and birth date, and the **checksum**. An invalid
   code is refused with a clear message.
3. A person cannot have two undecided applications for overlapping periods. The
   message says which application already exists, so it can be amended instead.
4. Verifying an application queries only the registries its scheme requires,
   and only the fields needed. Each result records its **source**, **retrieval
   time** and the registry's **source-data timestamp**, and is one of: verified
   positive, verified adverse, **unavailable** or **inconclusive**.
5. An unavailable or inconclusive result does not establish ineligibility: the
   application becomes **pending verification**. The worker can resolve such a
   check by **manual verification**, recording the outcome, the source used and
   a note.
6. The decision evaluates the verified facts against the scheme's rules only;
   a fact the scheme does not require is never a condition.
7. An application can be approved when every required fact is verified
   positive. If a required fact is verified adverse, approval is possible only
   as an **exception** with a written justification.
8. A rejection requires a **structured reason code**, an explanation of the
   material facts, the **legal basis** and information on how to challenge the
   decision. A rejection that cites a registry fact needs that fact to be
   verified adverse, not merely unavailable or inconclusive.
9. A decision cannot be changed or overwritten once recorded.
10. Searches by personal code, registry queries, manual verifications and
    decisions are written to an **audit log** with the user, time, application
    and action, without registry payloads.
11. The application list is searchable by personal code and filterable by
    status and benefit scheme.
