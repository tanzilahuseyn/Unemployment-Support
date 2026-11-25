# Work diary · Nilani Suraweera · legislation and policy analysis

Component: **SUPPORT.01, unemployment social support decided from X-Road registry checks**.
Document: [*User Story and Policy Analysis*](../User%20Story%20and%20Policy%20Analysis.pdf),
the group's legal and policy review of user story SUPPORT-US-001.


## Part 1 · Writing the analysis
| Date | Time (Estonia) | What I asked the agent | What it got wrong or missed | How it was corrected | Hours Spent |
|---|---|---|---|---|---|---:|
| 22 September 2026 | 09:30 (approx.) | Nilani | Review the gaps in the Estonian public services sector and identify a suitable e-governance project. | Used Gemini Pro to identify key gaps in Estonian public services and explored municipal procurement and blood donation coordination solutions. | Following team discussion, the municipal procurement solution was considered too complex, while the blood donation solution was rejected due to its complex legal and policy implications. The unemployment benefit registration portal was selected for further analysis. | 2.0 |
| 22 September 2026 | 14:00 (approx.) | Nilani | Review user story SUPPORT-US-001 (revision 1) against Estonian and EU law, and identify the main legal, policy and technical gaps. | The initial story treated registry checks as absolute conditions, meaning an unavailable registry could block an application. Validation relied only on the 11-digit personal identification code, and the system retained only a pass/fail result for each check. | Identified the critical principle that **"cannot verify" must not mean "not eligible."** The review also highlighted missing checksum validation, insufficient evidence records and the need to distinguish technical verification failures from eligibility decisions. | 2.5 |
| 22 October 2026 | 10:15 (approx.) | Nilani | Identify the legislation applicable to the unemployment benefit registration user story and list the relevant provisions. | Reviewed the legal basis for the proposed benefit and identified that the applicable legislation depends on the actual benefit scheme. The statutory subsistence benefit (*toimetulekutoetus*) is based on income and housing costs, rather than unemployment status alone. | The analysis assumed a municipal benefit administered by Tallinn, with cases handled by Mustamäe District Government. The exact benefit scheme and legal instrument remained to be confirmed. Sources included GDPR, the Social Welfare Act, Population Register Act, Taxation Act, Labour Market Services and Benefits Act, Administrative Procedure Act, Public Information Act and relevant X-tee / E-ITS guidance. | 2.0 |
| 23 October 2026 | 07:23 | Nilani | Map each applicable legal provision to a requirement and a corresponding design change (compliance tables A–D). | Reviewed the relationship between legal obligations and system requirements, including lawful data access, purpose limitation, data minimisation, transparency, decision justification, security and accountability. Some section references required confirmation against the applicable version of the legislation. | The compliance mapping was used to identify design changes and unresolved legal questions. The exact provisions should be verified against Riigi Teataja before the report is treated as a final legal assessment. | 3.0 |
| 24 October 2026 | 11:00 (approx.) | Nilani | Rewrite the acceptance criteria for SUPPORT-US-001 and prepare PowerPoint presentations for the policy analysis. | The original acceptance criteria lacked a sufficiently detailed evidence record, audit controls, reasoned rejection, correction or review mechanisms, and applicant-facing privacy information. Also prepared PowerPoint presentations to communicate the policy analysis, key legal findings, compliance gaps and proposed recommendations. | Developed 16 revised acceptance criteria. These separated verification from eligibility, introduced distinct verification outcomes, required structured decision reasons and legal bases, prevented silent decision overwrites, and addressed access logging, retention and applicant information. The PowerPoint presentations supported the communication and presentation of the policy analysis. | 2.5 |
| 25 October 2026 | 15:30 (approx.) | Nilani | Balance data minimisation with the need to provide evidence and explain an administrative decision. | Storing only `unemployment_check = pass` would not provide sufficient evidence to explain a disputed decision. Conversely, retaining complete registry responses could violate data minimisation and increase privacy risks. | Proposed a minimal evidence record for each check, including the result, relevant status, source, retrieval timestamp and the registry's own data timestamp. The retrieval time and source-data timestamp were treated as separate values. | 1.5 |
| 26 October 2026 | 16:00 (approx.) | Nilani | Review and finalise the policy analysis Word document before submission. | Identified editorial and structural issues: duplicate heading 4, an empty "Proposed verification model" section, a missing section 5, an unnumbered "Main findings" section, a stray "Refine the compliance report" line in question 5, and a user story header that still stated "Proposed." | Final editing was required to remove drafting artefacts, complete the missing sections, correct numbering and clarify the status of the user story. The report should distinguish confirmed legal findings from provisions and assumptions requiring further verification. | 1.5 |
| 15 November 2026 | Not recorded | Nilani | Revisit the policy analysis to identify additional legal provisions and assess the proposed solution from a broader management and policy perspective. | Re-examined the legal and regulatory framework to identify additional provisions and considerations that could affect the proposed system. Broadened the assessment to include governance, institutional responsibilities, accountability, implementation considerations and the implications for public-sector service delivery. | Strengthened the policy analysis beyond a purely legal compliance exercise by considering how the proposed system would operate within the wider public administration and governance environment. Identified areas requiring further legal and policy validation. | 2.0 |
| 25 November 2026 | Not recorded | Nilani | Conduct a further review of the policy analysis, focusing on additional legal provisions and management and policy implications. | Revisited the applicable legal framework and assessed whether additional provisions, governance requirements and policy considerations should be incorporated. Considered institutional accountability, administrative decision-making, implementation responsibilities and alignment with public-sector policy objectives. | Further refined the policy analysis by integrating additional legal, management and policy perspectives. Highlighted the importance of aligning technical system requirements with legal obligations, administrative responsibilities and broader public-sector governance objectives. | 2.0 |
| **Total** | | | | | **Total estimated effort** | **19.0 hours** |

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
