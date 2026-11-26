# Work diary · Tanzila Huseynova

Component: **SUPPORT.01, unemployment social support decided from X-Road registry checks**.
Agent: Claude Code (Claude Opus 5.5) in the Claude desktop app.

Written with the agent from the session history. Hours are estimated from commit
and pull request times, rounded to a quarter hour, with time added for reading,
discussing and reviewing. Total: **14 hours**.

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
| 11.11.2026 | 0.25 | Check the project against the course's submission requirements. | Two gaps were found: there was no reporting operation, and the list page had no detail panel, which the course's required minimum asks for. | I asked it to close both gaps. Defence and peer review were noted as my own tasks. |
| 11.11.2026 | 1 | Add the summary report and the list's detail panel (pull request #7). | Spec first and test 11 first, as before. The new test failed with an SQL error: a Java text block dropped a space, giving `anda.sys_status`. Docker Desktop was paused again, so the tests could not start. | The agent fixed the SQL and asked me to resume Docker. All 19 tests passed, and the report and panel were checked in the browser before merging. |
| 11.11.2026 | 0.25 | Give me an open link to share the app with a friend. | It asked how I wanted to share it instead of guessing. While restarting the frontend, it also stopped the backend's start script by accident: the stop command matched both. | The backend itself kept running; the agent checked this before going on. The link was tested from outside before I got it, with a warning that it works only while my laptop is on. |
| 12.11.2026 | 1 | I saw animal documents in my repository. Remove them. | The animal example was the teacher's template, and my code used some of its shared parts. It asked how far to go before deleting. | I chose to remove all animal parts (pull request #8). The shared parts moved to `config`, the database was renamed `support`, and the manuals were rewritten. One comment in an old migration still says "animals": the course rules forbid editing a migration that has already run. 16/16 tests passed. |
| 12.11.2026 | 0.5 | Explain what an API is and what my APIs are; then write a summary of the whole project for my teacher, also as a PDF. | The first draft of the summary said 14 endpoints and 13 changesets. | The agent counted them in the code before publishing: 13 endpoints and 16 changesets. |
| 23.11.2026 | 0.5 | Make a PowerPoint about my API. | It could not see the finished slides: Keynote and PowerPoint did not respond, and the macOS preview drew shapes in the wrong places. | It checked in code that nothing overflows or overlaps, shortened two long titles, and told me to look through the slides in PowerPoint before presenting. |
| 24.11.2026 | 0.5 | Make a diary for the policy side, from Nilani's *User Story and Policy Analysis*. | It found no record of how the analysis was written, so it would not invent dates, hours or prompts. It also found signs of AI drafting in the document: a duplicated heading, an empty section, a missing section 5 and a stray chat line. | It marked those places for Nilani to fill in, put the diary in the empty file she had created (renamed to `.md` so GitHub shows the tables), and linked her PDF (pull request #9). |
| 24.11.2026 | 0.25 | Nilani could not open my app link. | The old link had stopped working because the app had been shut down. | The agent restarted the app with a new link and tested it from outside. |
| 25.11.2026 | 0.25 | Is anything about the project left out? | This diary stopped at 23 October, and the README did not mention the summary report, the detail panel, the diaries or the policy analysis. | Both were updated in one pull request. Still open: Nilani's diary details, the defence and the peer review. |
| 25.11.2026 | 0.25 | Fix the spec gap and delete the 11 old branches. | The spec named changesets 04–08 but not 91 or 09. One old branch had a commit that was not on `main`. | The spec now names every changeset (pull request #11). Before deleting, the agent checked that commit: it was the 18 October README, which had reached `main` through pull request #3, so nothing was lost. |
| 25.11.2026 | 0.5 | What should I do for the defence? Then: slides for my part (the app, the API, X-Road and the demo). | It still could not see the finished slides on my computer. | It checked in code that nothing overflows or overlaps, added speaker notes to every slide, and opened the deck in PowerPoint for me to look through. |
| 26.11.2026 | 0.25 | Give Nilani a new app link. | Docker and the app had stopped, so the old link was dead. | It started Docker, the app and a new link, and tested the link from outside. |
| 26.11.2026 | 1 | Change the website: a darker background, a title a citizen understands, "number of family members" instead of "household", and name, surname and father's name after the personal code (pull request #12). | Darkening the page colour changed nothing visible: the white area was the card colour. Names in the list would have broken our privacy rule, and the Estonian Population Register has no father's name. | It darkened the card colour instead. It explained the privacy conflict and gave options. We chose the first name and surname next to the personal code on the application page, looked up live and never stored, plus the date of birth from the code. Spec first and business test 12 first; 17/17 tests passed. Father's name was left out, because the personal code already tells people with the same name apart. |
| 26.11.2026 | 0.25 | Update my slides, the summary and this diary for the new numbers (14 endpoints, 12 business tests). | The files used to build the slides had been deleted from the agent's temporary folder. | It edited my saved slides directly, kept the changes I had made in PowerPoint, and saved a backup of each deck first. |
| 26.11.2026 | 0.25 | I saw 7 cases. Give the demo people full names and realistic birth dates. | The new date-of-birth column showed the demo codes' birth dates (01.01.1990, 02.02.1990, …), which looked obviously made up. The 7th case had been typed in through the shared link, and its personal code might belong to a real person. | New valid codes with varied birth dates and full names, through a new demo changeset (92) instead of editing an old one (pull request #14). The 7th person was not added to the imitated registries; the database was reset to the 6 demo cases. The slides, summary and PDF got the new codes. |
| 26.11.2026 | 0.25 | Change the list title to "Assignment of unemployment insurance payment". | "Unemployment insurance" is the Unemployment Insurance Fund's own benefit, not the city's support this app handles. The policy analysis stresses the difference, so a teacher could call it the wrong benefit. | The agent explained this before changing anything. I chose **"Assignment of unemployment support payment"** (pull request #15), and the spec says why. |

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
- **Check the numbers before presenting.** The summary's first counts were
  wrong; counting in the code fixed them.
- **A diary must not be invented.** When the agent did not know how Nilani
  worked, it left the gaps for her instead of guessing.
- **Keep the diary up to date as you go.** Filling in weeks later relies on
  commit times and memory.
- **A small wish can clash with a design rule.** Showing names in the list
  sounded simple, but it went against our privacy rule. The agent raised it
  before building, and we found a way that keeps both.
- **Words have legal meaning.** "Insurance" and "support" are different
  benefits in Estonian law, so a title is part of the design too.
