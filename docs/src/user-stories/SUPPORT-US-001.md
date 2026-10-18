# SUPPORT-US-001 · Decide unemployment support from live registry data

> **SUPPORT-US-001** · state **Confirmed** · traces to [SUPPORT.01](../specifications/SUPPORT.01-unemployment-support.md)

**As** a social worker in a local government
**I want** to register an unemployed resident's application for social support
and see, in one place, what the state registries say about them right now
**so that** I can decide fairly and quickly, without asking the applicant for
paper certificates the state already holds.

## Context

People who lose their job can apply to their local government for social
support. Before deciding, the social worker must confirm three facts that
live in three different state registries:

- the applicant **lives in this municipality**: the Population Register
  (Rahvastikuregister);
- the applicant is **registered as unemployed**: the Unemployment Insurance
  Fund (Töötukassa);
- the applicant has **no active employment**: the Employment Register
  (töötamise register) of the Tax and Customs Board.

Today the applicant often brings printouts, or the worker logs in to several
systems. Estonia's "once only" principle says the state should not ask a
person for data it already has. This component asks the registries over
X-Road (imitated in this course) and shows the answers side by side.

Registries can be out of date or unreachable, and real lives don't always fit
the rules: someone may have just moved, or a job may have ended yesterday.
So the worker can **override** a failing or unavailable check, but only with
a written justification that is kept with the decision.

**Privacy by design.** Unemployment and income are sensitive personal data.
The component stores only what the decision needs: the personal code, the
application and the decision. Names, addresses and registry answers are
looked up live and never stored, so they cannot go stale or leak from this
database. Only the outcome of each check that justified a decision is
recorded with it, as the worker saw it at the moment of deciding.

Out of scope: paying the support, calculating the amount from income,
appeals, the applicant's own self-service portal, and other support types
than unemployment-related support.

## Acceptance criteria

- The worker can register an application with the applicant's personal code,
  the support type, the period it covers and the household size.
- A personal code is 11 digits and is refused with a clear message when it
  is malformed.
- A person can have only one open application for overlapping periods.
- For an application, the worker sees live from the three registries:
  name and residence, unemployment registration and its start date, and
  whether employment is active. If a registry is unreachable, that check is
  shown as unavailable and the rest of the page still works.
- The worker can approve or reject an application; a rejection requires a
  reason. A decided application can no longer be edited.
- An application whose three checks all pass can be approved directly. If any
  check fails or is unavailable, it can be approved only as an **override**
  with a written justification.
- When a decision is made, the system records who decided, when, the outcome
  of each check at that moment, and whether it was an override; it does not
  store the registries' full answers.
- The application list is searchable by personal code and filterable by
  status (open, approved, rejected) and support type.
