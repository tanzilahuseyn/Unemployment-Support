# Manual · REST API

The API layer in one page: how a request travels through the backend, what
every list, error and identifier looks like, and how you add to it. The
endpoint table itself is in the
[specification](../specifications/SUPPORT.01-unemployment-support.md#api);
the living contract is **Swagger UI at <http://localhost:18440/swagger-ui.html>**
(OpenAPI JSON at `/v3/api-docs`).

## Where things are

Package `ee.taltech.ite4120.support`, one directory per layer:

| Directory | Contains | Rule |
| --- | --- | --- |
| `api/` | `ApplicationController`, `ApplicationRequest`, `DecisionRequest`, `ManualVerificationRequest` (request bodies + Bean Validation) | **HTTP only** — a business rule in a controller is a defect |
| `service/` | `ApplicationService`, `RegistryCheckService`, `AuditService`, `PersonalCode` | the business rules, one method per use case, `@Transactional` on writes |
| `repository/` | `ApplicationRepository extends BaseRepository`, `EvidenceRepository`, `DecisionRepository`, `AuditRepository`, `ReportRepository` | SQL, row mapping, pagination — nothing else |
| `model/` | `Application`, `Decision`, `Evidence`, `BenefitScheme`, `AuditEvent`, `SummaryReport`, the enums | plain Java objects mapping onto tables (including `sys*` fields, read-only) |
| `dto/` | `ApplicationQueryParams extends QueryParams` | the list endpoint's filters |
| `registry/` | three registry adapters (`population`, `unemployment`, `employment`), each with HTTP and X-Road transports and wire records, plus the imitated registries | external data goes through an interface |

Shared by the whole application, in `ee.taltech.ite4120.config`:
`ApiErrorHandler` (the error model), `MockUmaController` (mock sign-in),
`SecurityConfig`, `MockBearerAuthFilter`, `JdbcConfig`, `OpenApiConfig`.

A request runs controller → service → repository and back; exceptions thrown
anywhere are turned into a problem document by `ApiErrorHandler`.

## Required minimum

What the API must have, and where this component does it:

| Requirement | In this component |
| --- | --- |
| The layers above, in the component's own package | `support/` |
| Create · read one · list · update · withdraw (soft delete) for the main entity | `POST` 201 + `Location` · `GET /{id}` · `GET` paged · `PUT` · `DELETE` 204 on `/api/support/applications` |
| A reporting operation | `GET /api/support/reports/summary?from=&to=` — aggregate counts only |
| The platform list contract: `limit`, `offset`, `sort`, and `{ data, meta }` in the answer | `ApplicationQueryParams`, `QueryResult<Application>` |
| **Every error is `application/problem+json`**; 4xx is the caller's fault, 5xx is ours | `ApiErrorHandler` |
| Validation in three places: request shape, business rules, database | `ApplicationRequest` → `ApplicationService.validate` (with the personal code checksum) → constraints in `02-application.sql` |
| External registries behind adapter interfaces, with a **stated failure policy** | `PopulationRegistryAdapter`, `UnemploymentRegistryAdapter`, `EmploymentRegistryAdapter` — a failed call is recorded as `UNAVAILABLE` evidence, never as an eligibility result |
| Documented in Swagger with `@Operation` / `@ApiResponse` | `ApplicationController` |
| The specification's business tests executable against the service | `SupportBusinessRulesIT` |

## Calling it

Local mode uses mock authentication: the Bearer token **is** the username,
nothing is verified (`auth.mock.enabled=true` in the `local` profile —
`MockBearerAuthFilter`). Without a token every `/api/**` call answers 401.
The name in the token is also who the database records: `sys_created_by` and
`sys_modified_by` carry it (`JdbcConfig` hands it to `core.set_user()` on every
connection a request uses).

```bash
H='Authorization: Bearer superadmin'
curl -s -H "$H" 'http://localhost:18440/api/support/applications?limit=2&sort=-periodStart'
curl -s -H "$H" -H 'Content-Type: application/json' \
     -d '{"applicantIsikukood":"39001010011","supportTypeCode":"ONE_OFF","periodStart":"2027-01-15","periodEnd":"2027-01-15","householdSize":1}' \
     -i http://localhost:18440/api/support/applications      # 201, Location: /api/support/applications/<id>
curl -s -H "$H" -X POST http://localhost:18440/api/support/applications/<id>/verification
curl -s -H "$H" 'http://localhost:18440/api/support/reports/summary?from=2026-10-01&to=2026-12-31'
curl -s -H "$H" -X DELETE -i http://localhost:18440/api/support/applications/<id>   # 204, soft delete
```

## The list contract

Every list endpoint takes the platform's `QueryParams` plus its own filters:

| Parameter | Meaning |
| --- | --- |
| `limit`, `offset` | page size and start — `?limit=20&offset=40` |
| `sort` | `sort=periodStart` ascending, `sort=-periodStart` descending; only keys the repository maps are allowed (`periodStart`, `applicantIsikukood`) — anything else is a 400 |
| `textContains`, `status`, `supportTypeCode` | the endpoint's own filters, declared in `ApplicationQueryParams` |

and answers a `QueryResult`:

```json
{
  "data": [ { "id": 1000, "applicantIsikukood": "39001010011", "supportTypeCode": "MONTHLY", "periodStart": "2026-10-01", "periodEnd": "2026-10-31", "householdSize": 1, "status": "OPEN", "sysVersion": 1, "…": "…" } ],
  "meta": { "total": 6, "offset": 0, "itemsPerPage": 20, "pages": 1 }
}
```

The repository implements it in one line — `query(params, this::count, this::list)`
— with a count query and a page query driven by the same `SqlBuilder` filter.

## Errors

One shape for everything, RFC 9457 `application/problem+json`:

```json
{ "status": 400, "title": "Invalid request", "detail": "applicantIsikukood is not a valid Estonian personal code", "instance": "/api/support/applications" }
```

| Status | Meaning | Throw |
| --- | --- | --- |
| 400 | the request is wrong in itself: shape (Bean Validation) or a rule about the message (invalid personal code, period end before start, a decision without its reason, an unknown sort key) | `ApiClientException` |
| 404 | no active application with that id, or no decision yet | `NotFoundException` |
| 409 | the current state forbids it (the application is already decided; an overlapping undecided application exists) | `ConflictException` |

A registry that cannot answer is **not** an API error: the check is stored as
`UNAVAILABLE` evidence and the application waits in `PENDING_VERIFICATION`
until it is verified again or manually.

`ApiErrorHandler` is a `@RestControllerAdvice`: one method per exception type,
each returning a `ProblemDetail`. Never a stack trace, never framework HTML.
The commons exceptions come from `org.helex.commons.exception`.

## Identity and lifecycle

- Ids are `Long`, minted by the database; the client never sends one on create.
- The personal code (`applicantIsikukood`) is a **string** in the model, the
  request and the database, and it is **immutable** after creation — `PUT`
  keeps the stored one.
- Only undecided applications change: `PUT` returns the application to `OPEN`,
  so it must be verified again. A decided application answers 409.
- Withdraw is a soft delete: `DELETE` answers 204 and sets `sys_status = 'C'`;
  every read filters on `'A'`.
- `sys*` fields (status, version, created at / by, modified at / by) appear in
  responses but are never accepted on input; the database trigger owns them.

## The registry adapters

Each registry has a port with one method, `lookup(isikukood)`:
`PopulationRegistryAdapter` (residence), `UnemploymentRegistryAdapter`
(Töötukassa registration), `EmploymentRegistryAdapter` (active employment).
Which transport answers is configuration, `support.registry.mode`:

| Mode | Classes | When |
| --- | --- | --- |
| `http` (default) | `Http…RegistryAdapter`, through `RegistryHttpClient` | plain HTTPS+JSON against `support.registry.url`; locally that is this application's own imitated registries, `GET /mock-registry/{population,unemployment,employment}/{isikukood}` (`MockSupportRegistriesController`, local profile only) |
| `xroad` | `XRoad…RegistryAdapter`, through `XRoadRegistryClient` | the forge-xroad transport against a security server — a compiling skeleton that fails fast and clearly without one |

Two-layer rule: the **wire records** (`RrElukohtResponse`, `TkTootuResponse`,
`TorTootamineResponse` and their X-Road counterparts) keep the provider's
Estonian field names verbatim so they stay diffable against the provider's
schema; the **mapper** in each adapter turns them into our own records. Never
store what a registry owns — only the check result, its source and times are
kept as evidence.

**Failure policy**, stated in the spec: an unreachable registry makes the check
`UNAVAILABLE`, never `FAIL`. "Cannot verify" does not mean "not eligible".

## Adding an endpoint

1. Model + query params in `model/`, `dto/`.
2. Repository: `extends BaseRepository`, `PgBeanProcessor` or a row mapper,
   `SqlBuilder` for filters, `query(...)` for lists, `sys_status = 'A'` in
   every read.
3. Service: rules from the specification's business tests, throwing the
   commons exceptions; `@Transactional` on writes.
4. Controller: `@RestController @RequestMapping("/api/support")`, `@Valid`
   request bodies, `@Operation` per method; 201 + `Location` on create, 204 on
   withdraw.
5. Errors: reuse the commons exceptions; add a handler to `ApiErrorHandler`
   only for an exception of your own.
6. Check it in Swagger, then with `curl`, then in the browser.

References: `ApplicationController.java` · `ApplicationService.java` ·
`ApplicationRepository.java` · `config/ApiErrorHandler.java` ·
[the API section of the spec](../specifications/SUPPORT.01-unemployment-support.md#api)
· `AGENTS.md` § Architecture conventions.
