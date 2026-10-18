# Unemployment Support

A case register for a local government's social workers: register an
unemployed resident's application for social support, see **live** what three
state registries say about them, and approve or reject it, without asking the
applicant for paper certificates the state already holds.

An ITE4120 course project, built on the course's e-government-shaped template
([igorboss/ite4120](https://github.com/igorboss/ite4120)): Spring Boot,
PostgreSQL with Liquibase, React with the Helex component library, and three
registry lookups over X-Road, imitated locally.

## What it does

**Applications** (`/support`)
- Register an application with the applicant's personal code, support type
  (one-off or monthly), period and household size.
- Search by personal code; filter by status (open, approved, rejected) and
  support type.
- Edit or withdraw an application while it is open. A decided application can
  no longer change.
- A person can have only one open application for overlapping periods.

**Three live X-Road checks** on every application page:

| Check | Registry | Passes when |
| --- | --- | --- |
| Residence | Population Register (Rahvastikuregister) | the person lives in this municipality |
| Unemployment | Unemployment Insurance Fund (Töötukassa) | the person is registered as unemployed |
| Employment | Employment Register (töötamise register, EMTA) | the person has no active employment |

Each check is **PASS**, **FAIL** or **UNAVAILABLE** (the registry could not be
reached). A registry being down never breaks the page.

**Decisions**
- Approve directly when all three checks pass.
- If a check fails or is unavailable, approve only as an **override** with a
  written justification.
- A rejection always needs a reason.
- The server runs the checks again when deciding and stores who decided, when,
  and each check's outcome at that moment.

**Private by design:** only the personal code, the application and the
decision are stored. Names, addresses and registry details are looked up live
each time and never stored.

## Specification

Spec first, tests second, implementation third (see `AGENTS.md`):

- User story: [`docs/src/user-stories/SUPPORT-US-001.md`](docs/src/user-stories/SUPPORT-US-001.md)
- Specification: [`docs/src/specifications/SUPPORT.01-unemployment-support.md`](docs/src/specifications/SUPPORT.01-unemployment-support.md)
  covers the data model, API, UI, failure policy and business tests.

## Where the code is

| Layer | Where |
| --- | --- |
| Database: PostgreSQL + Liquibase, schema `support` | `backend/src/main/resources/support/db/changelog/` |
| API and business rules: Spring Boot + springdoc | `backend/src/main/java/ee/taltech/ite4120/support/` |
| Registry adapters (HTTP, X-Road skeleton, mocks) | `.../support/registry/` (`population`, `unemployment`, `employment`) |
| UI: React + @helex/ui | `frontend/src/pages/support/`, `frontend/src/supportApi.ts` |

The template's worked example, the **animals register**, is still included
(`/animals`, package `ee.taltech.ite4120.animals`) as the pattern this
component follows.

## Run it

First time on this machine? Install the tools and set up the GitHub token
first ([Tools](#tools), below). Then:

```bash
# 1. database + backend (the first run downloads dependencies, which takes a few minutes)
./scripts/run-backend.sh          # Windows: .\scripts\run-backend.ps1

# 2. frontend, in a second terminal
cd frontend && npm ci && cd ..
./scripts/run-frontend.sh         # Windows: .\scripts\run-frontend.ps1
```

| What | Where |
| --- | --- |
| Applications | http://localhost:18640/support |
| Swagger UI, the live API contract | http://localhost:18440/swagger-ui.html |
| Imitated registries | http://localhost:18440/mock-registry/population/39001010011 (also `/unemployment/…` and `/employment/…`) |
| PostgreSQL | localhost:18520 (`scripts/psql` for a shell) |
| Documentation book | `scripts/run-docs` → http://localhost:18740 (needs mdBook) |

You are signed in automatically as a mock user. The API accepts
`Authorization: Bearer <any-username>` in local mode. The municipality the
component serves is `support.municipality` in `application.yml` (Tallinn).

**Demo data** (loaded locally under the Liquibase `demo` context): one open
application for each of four **fictional** people the imitated registries
know, one per case:

| Personal code | Person | Checks |
| --- | --- | --- |
| 39001010011 | Kalle Kask | all pass |
| 49002020022 | Liis Lepp | residence fails: lives in Tartu |
| 39003030033 | Mart Mets | employment fails: has an active employment |
| 49004040044 | Anu Allik | unemployment fails: not registered |

The database is disposable: `scripts/reset-db` destroys and recreates it, and
Liquibase rebuilds the schema on the next backend start.

## Tests and checks

Each line runs from the project folder:

```bash
(cd backend && ./gradlew test)      # business tests, against a throwaway PostgreSQL
(cd frontend && npx tsc -b)         # typecheck
python3 scripts/check_changesets.py # Liquibase formatted-SQL rules
```

`SupportBusinessRulesIT` holds the specification's seven business tests:
- personal code format
- period order
- one open application per person for overlapping periods
- a rejection needs a reason
- approve directly when all checks pass, after which the application is closed
- a failing check needs an override with a reason
- an unreachable registry is unavailable, not fatal

The three registries are test doubles, so each case is set up exactly.
`XRoadSkeletonsTest` checks that each X-Road adapter fails fast and clearly
when no security server is reachable.

The same checks run on every pull request as the **Verify** workflow
(`.github/workflows/verify.yml`).

## License

Apache License 2.0, inherited from the course template. See [`LICENSE`](LICENSE).

## Tools

What the scripts and builds in this repository actually call, and why:

| Tool | Version | Used for |
| --- | --- | --- |
| **Git** | any recent | clone, branch, PR — your contribution is the history under your name (gate B) |
| **Docker Desktop** (or a compatible engine with `docker compose`) | running | PostgreSQL locally (`scripts/run-backend`, `reset-db`, `psql`); a throwaway PostgreSQL in the tests (Testcontainers) |
| **JDK 25** | 25 | the Gradle wrapper runs on it *and* compiles with it — nothing downloads a JDK for you (no toolchain resolver is configured), so it must be installed |
| **Gradle** | 9.4 | provided by the wrapper: `backend/gradlew` downloads the pinned version on first use. Nothing to install; a second, system-wide Gradle only invites version mix-ups |
| **Node.js** + npm | 20+ | the Vite frontend (`scripts/run-frontend`, `npm ci`, `npx tsc -b`) |
| **Python 3** | 3.x | `scripts/check-changesets` — the formatted-SQL grammar gate (rule 6 in `AGENTS.md`) |
| **GitHub account** + token with `read:packages` | — | the Helex packages live in GitHub Packages, which refuses anonymous reads |
| GitHub CLI `gh` | optional | pull requests from the terminal; not needed for the token |
| mdBook | optional | serves `docs/` as a book — `scripts/run-docs` |

No local `psql` needed — `scripts/psql` runs it inside the container.

Docker engines other than Docker Desktop (Colima, Rancher Desktop, Podman) work
for running the app, but the tests need to be told where the socket is — with
Colima: `export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` before
`./gradlew test`.

### Install with a package manager

One script per platform, idempotent, safe to re-run. Each installs the
package manager itself if it is missing, then the rows above, then prints a
version check:

```bash
# macOS — Homebrew
./scripts/setup-tools.sh
```

```powershell
# Windows — Chocolatey, in PowerShell run as Administrator
.\scripts\setup-tools.ps1
```

By hand, the same thing:

| | macOS (Homebrew) | Windows (Chocolatey) |
| --- | --- | --- |
| Git, GitHub CLI | `brew install git gh` | `choco install -y git gh` |
| JDK 25 | `brew install openjdk@25` — keg-only: register it with `sudo ln -sfn "$(brew --prefix openjdk@25)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-25.jdk` | `choco install -y temurin25` (sets `JAVA_HOME` and `PATH`) |
| Node.js + npm | `brew install node` | `choco install -y nodejs-lts` |
| Python 3 | `brew install python` | `choco install -y python` |
| Docker Desktop | `brew install --cask docker-desktop` | `choco install -y docker-desktop` — needs WSL 2: `wsl --install` once, reboot |
| mdBook (optional) | `brew install mdbook` | no Chocolatey package — see the [mdBook installation guide](https://rust-lang.github.io/mdBook/guide/installation.html) |

Windows notes: run the repository's PowerShell scripts as `.\scripts\name.ps1`;
if PowerShell refuses to run scripts, `Set-ExecutionPolicy -Scope CurrentUser
RemoteSigned` once. Docker Desktop must be started once by hand after install
(it finishes the WSL 2 setup on first start). winget and Scoop carry the same
packages if you prefer them.

### GitHub token

The one step nothing works without: the Helex packages live in GitHub
Packages, which refuses anonymous reads.

**Create the token** at
**[github.com/settings/tokens/new — pre-filled for this project](https://github.com/settings/tokens/new?description=ITE4120+Helex+packages+read-only&scopes=read:packages)**
— the form opens with exactly the one scope this project needs; pick an
expiration, press *Generate token*, copy it once. From a terminal:

```bash
open "https://github.com/settings/tokens/new?description=ITE4120+Helex+packages+read-only&scopes=read:packages"
```

(Windows: paste the URL into the browser, or `start ""` instead of `open`.)
It must be a **classic** token: GitHub's Maven and npm registries do not
accept fine-grained tokens. `read:packages` alone is the minimum privilege —
it can download packages your account can see, and nothing else.

**Both package managers need it.** Gradle reads it from
`~/.gradle/gradle.properties`; npm reads it from its global config — and
GitHub's npm registry refuses anonymous reads **even for public packages**,
so `npm install` in `frontend/` fails with a 401 until this has run once:

```bash
npm config set //npm.pkg.github.com/:_authToken=ghp_YOUR_TOKEN --global
```

**Wire it in** — one paste, silent prompt, no token in your shell history
(macOS/Linux; on Windows edit the two files by hand as shown below):

```bash
read -s TOKEN && printf 'gpr.user=%s\ngpr.key=%s\n' "YOUR_GITHUB_USERNAME" "$TOKEN" >> ~/.gradle/gradle.properties && npm config set "//npm.pkg.github.com/:_authToken=$TOKEN" --global && unset TOKEN
```

What that writes, if you prefer to do it by hand:

```properties
# ~/.gradle/gradle.properties  (backend)
#   Windows: %USERPROFILE%\.gradle\gradle.properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=ghp_YOUR_TOKEN
```

```bash
# npm (frontend) — one command, same on Windows:
npm config set //npm.pkg.github.com/:_authToken=ghp_YOUR_TOKEN --global
```

Missing or wrong token looks like this: Gradle says *cannot resolve
org.helex.emr:…*, npm says *401*. Fix the credentials first — never vendor
jars. No other access is needed: the backend's Helex jars resolve from the
course repository's Maven registry, `igorboss/ite4120` (mirrored by the
lecturer with `scripts/mirror-packages.sh`), and the `@helex-solutions` npm packages are
public — GitHub's npm registry still wants the token to read them, but no
extra permission. A correct token that still gets 401/403 means the token is
expired or your account has no access to `igorboss/ite4120`; ask the
lecturer.
