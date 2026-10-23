# Work diary · Tanzila Huseynova

Component: **SUPPORT.01, unemployment social support decided from X-Road registry checks**.
Agent: Claude Code (Claude Opus 5.5) in the Claude desktop app.

Written with the agent from the session history. Hours are estimated from commit
and pull request times, rounded to a quarter hour, with time added for reading,
discussing and reviewing. Total: **6.75 hours**.

| Date | Hours | What I asked the agent | What it got wrong or missed | How it was corrected |
| --- | --- | --- | --- | --- |
| 18.10.2026 | 0.75 | Suggest a project topic linked to X-Road, then write a user story for social support for unemployment. | Its first story did not allow any approval while a registry check failed. That is too strict: registries can be out of date, and real lives don't always fit the rules. | I asked for an **override with a written justification**. The story was changed before anything was built. |
| 18.10.2026 | 0.25 | Keep the blood project, but put this one in a separate repository. | It had already started saving the new story and spec into the blood repository. | It moved them out of Blood-Donation, which it left clean, and created **Unemployment-Support** from a clean copy of the course template. nilani-suraweera was invited with write access. |
| 18.10.2026 | 0.5 | Write the specification SUPPORT.01 and confirm it. | Nothing wrong. It left the support types and the use of household size open for the group. | I confirmed the spec, and pull request #1 was merged. |
| 18.10.2026 | 0.25 | Write the business tests before the implementation. | Nothing wrong. All seven tests failed to compile first, because no code existed yet. | — |
| 18.10.2026 | 0.5 | Implement the database, backend and the three registry adapters (Population Register, Töötukassa, Employment Register). | It worried that a date library might be missing in Spring Boot 4 and changed the plan before compiling. | Dates are sent as text and parsed in the mapper. All seven tests passed on the first run. |
| 18.10.2026 | 0.5 | Build the UI pages and show me how the app looks. | Nothing wrong in the pages. The browser check showed every check result and the override path working. | — |
| 18.10.2026 | 0.25 | Change "Created by: animals" to "social services". | "animals" came from the template, and the agent only mentioned it instead of fixing it. | I chose the name. It changed both places, reset the demo database and checked the page. |
| 18.10.2026 | 0.25 | The README still talked about animals. | It pushed the new README to pull request #2 just after I had merged it, so the README never reached `main`. | It found this, put the README in its own pull request (#3), and merged it after the checks passed. |
| 18.10.2026 | 0.25 | The user story and spec looked empty on GitHub. | Nothing wrong: I was looking at `docs/_templates`, the teacher's blank templates. | It showed me where the real documents are: `docs/src/user-stories` and `docs/src/specifications`. |
| 23.10.2026 | 1 | Compare our project with our group's legislation and policy analysis, and adjust it. | Revision 1 treated an unreachable registry as a reason to block approval, stored only PASS or FAIL, and checked only that a personal code has 11 digits. | We chose: the **core changes**, a **municipal benefit** rather than the subsistence benefit, and household size as **context only**. Legal paperwork (DPIA, retention, roles) is listed in the spec as outside the code. |
| 23.10.2026 | 0.5 | Rewrite the user story and spec as revision 2 and confirm them. | Three of the four demo personal codes failed the official checksum. | The spec replaces them with valid codes through a new changeset, and I confirmed revision 2. |
| 23.10.2026 | 0.25 | Rewrite the business tests for revision 2 before the code. | Nothing wrong. All ten tests failed to compile first. | — |
| 23.10.2026 | 1 | Implement revision 2: evidence, pending verification, manual verification, reasoned decisions and the audit log. | The tests could not run: Docker Desktop was paused. The agent first thought it might be a startup problem. | It read the real error and asked me to unpause Docker. After that, all ten tests passed. |
| 23.10.2026 | 0.5 | Check revision 2 in the real app and merge it. | Upgrading my existing database failed: an old rejection had no reason code, which revision 2 requires. The audit log was also in the wrong order. | Old rejections are marked `OTHER` with their text kept, and the log is sorted by time. The browser check then passed (Eve: unavailable → manual verification → approved), and pull request #5 was merged after the checks were green. |

## What I learned about working with the agent

- **Say which repository.** The agent started work in the blood repository
  before I said this project needed its own.
- **A legal review changes the design, not only the text.** The group's analysis
  showed that "cannot verify" must not mean "not eligible". That changed
  the statuses, the database and the tests.
- **Tests from an empty database are not enough.** Upgrading a database that
  already had data found a bug the ten tests could not.
- **Check the real app.** The browser check found problems the tests missed:
  the template's "animals" user, and the audit log order.
- **Merge only after the checks pass**, and check that the change actually
  reached `main`.
