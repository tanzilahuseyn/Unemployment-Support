# Work diary · Nilani Suraweera · legislation and policy analysis

Component: **SUPPORT.01, unemployment social support decided from X-Road registry checks**.
Document: *User Story and Policy Analysis* (Word), the group's legal and policy review
of user story SUPPORT-US-001.

This diary was drafted from the finished document and the repository history.
Marks like **[Nilani: …]** are facts only Nilani knows: the dates, the hours and the
exact questions she asked the AI tool. She fills them in before submission. Nothing
in those places has been guessed.

## Part 1 · Writing the analysis

| Date | Hours | What I asked the agent | What it got wrong or missed | How it was corrected |
| --- | --- | --- | --- | --- |
| [Nilani: date] | [Nilani] | Review user story SUPPORT-US-001 (revision 1) against Estonian and EU law, and list the main problems. [Nilani: the AI tool and the prompt] | The story treated the three registry checks as absolute conditions: an unreachable registry blocked approval. It also checked only that a personal code has 11 digits, and kept just a pass/fail result per check. | The review named the critical finding: "cannot verify" must not mean "not eligible". It also listed the missing checksum validation and the too-thin evidence record. |
| [Nilani: date] | [Nilani] | List the legislation that applies, with the relevant sections. [Nilani: prompt] | The legal basis depends on which benefit this is. The statutory subsistence benefit (toimetulekutoetus) is calculated from income and housing costs, not from unemployment status. A generic "unemployment support" label does not define any eligibility rules. | The analysis assumes a municipal benefit run by Tallinn, with cases handled by Mustamäe District Government, and states that the real legal instrument must still be confirmed. It cites GDPR, the Social Welfare Act, the Population Register Act, the Taxation Act (Employment Register), the Labour Market Services and Benefits Act, the Administrative Procedure Act, the Public Information Act and the X-tee/E-ITS guidance. |
| [Nilani: date] | [Nilani] | Map each provision to a requirement and a design change (compliance tables A–D). [Nilani: prompt] | Several section numbers are given only "of the applicable version", and one entry says "verify the current rules". The tool did not confirm them against the law in force. | **[Nilani: say whether you checked them in Riigi Teataja, and what you changed.]** The project's specification lists the same laws as sources and does not rely on the exact section numbers. |
| [Nilani: date] | [Nilani] | Rewrite the acceptance criteria. [Nilani: prompt] | The original criteria had no evidence record, no audit, no reasoned rejection, no correction route and no privacy notice. | 16 revised acceptance criteria: verification separated from eligibility, four kinds of result, a structured reason with legal basis, decisions that cannot be silently overwritten, access logging, retention rules and applicant information. |
| [Nilani: date] | [Nilani] | Balance data minimisation with being able to explain a decision. [Nilani: prompt] | Storing only `unemployment_check = pass` cannot explain a disputed decision, but storing the full registry answer is too much. | A minimal evidence record per check: the result, the relevant status, the source, the retrieval time and the registry's own data timestamp. These two times are kept apart. |
| [Nilani: date] | [Nilani] | Final editing of the Word document. | The finished document still shows signs of AI drafting: <br>• heading 4 appears twice; <br>• "Proposed verification model" is empty; <br>• there is no section 5; <br>• "Main findings" has no section number; <br>• question 5 contains the stray line "Refine the compliance report", which looks like a chat suggestion copied in; <br>• the story header still says *Proposed*. | **[Nilani: fix these in the document, or note here why they stay.]** |

## Part 2 · How the analysis changed the software

These steps are recorded in the repository and in Tanzila's diary
(`docs/diary/tanzila-huseynova.md`). They show the analysis was used, not only written.

| Date | Hours | What happened | What the analysis caught | Result in the code |
| --- | --- | --- | --- | --- |
| 23.10.2026 | 1 (Tanzila) | The project was compared with this analysis, and the group answered the document's open decisions. | Questions 1, 4 and 5 in section 7 were open. | **Benefit:** a separate municipal benefit, not the subsistence benefit. **Unavailable check:** the application stays pending for verification. **Household size:** context only. |
| 23.10.2026 | 0.5 (Tanzila) | The user story and specification were rewritten as revision 2 and confirmed (pull request #4). | The 11-digit check was too weak. With the checksum rule, three of the four demo personal codes turned out to be invalid. | Checksum validation; valid demo codes added in a new database migration. |
| 23.10.2026 | 1 (Tanzila) | Revision 2 was built (pull request #5). | All findings that can be built in code. | <br>• Benefit schemes linked to their legal basis and rule version. <br>• Four check results. <br>• Status PENDING_VERIFICATION, with manual verification that needs a source and a note. <br>• Exceptions only for a verified adverse fact. <br>• Reason codes and legal basis on every rejection. <br>• Evidence records without names or addresses. <br>• An audit log that cannot be changed. <br>• 10 business tests that check these rules. |

### What stayed outside the code, and why

The specification's section *Outside the code* lists the analysis's recommendations
that are organisational or legal work rather than software:
- confirming the legal instrument and its rules;
- registry access agreements and an X-tee subsystem;
- staff roles and the procedure for reopening decisions;
- a DPIA screening, a privacy notice and a retention schedule;
- monitoring and incident procedures.

This keeps them from being mistaken for done.

## What I learned about working with the agent

- **The AI answered the general question well, but not "which benefit is this".**
  The biggest correction came from asking which legal instrument applies. That
  question changed the whole design.
- **Section numbers need checking.** Wording like "of the applicable version" means
  the tool was unsure. Every section number must be checked in Riigi Teataja.
- **Edit the final document by hand.** Duplicated headings, empty sections and
  leftover chat text are easy to miss in a long AI-assisted document.
- **A legal review is useful only when it reaches the code.** Each critical
  finding became a rule, a status or a test in revision 2.
  [Nilani: add your own lessons.]
