# Work diary · Nilani Suraweera · legislation and policy analysis

Component: **SUPPORT.01, unemployment social support decided from X-Road registry checks**.
Document: [*User Story and Policy Analysis*](../User%20Story%20and%20Policy%20Analysis.pdf),
the group's legal and policy review of user story SUPPORT-US-001.


| **Date** | **Hours** | **What happened** | **What the analysis caught** | **Result in the code** |
| -------- | ---------: | --------------- | ---------------------------- | ---------------------- |
| 22 September 2026 | 2.0 | Reviewed gaps in Estonia's public services sector and explored potential e-governance project ideas, including municipal procurement and blood donation coordination. Following team discussion, selected the unemployment benefit registration portal. | Municipal procurement was too complex for the project scope, while blood donation coordination involved complex legal and policy implications. The unemployment benefit registration process offered a more suitable opportunity to explore digital public services and registry integration. | Established the project direction and selected the unemployment benefit registration portal as the focus for further analysis and development. |
| 22 September 2026 | 2.5 | Reviewed user story SUPPORT-US-001 (revision 1) against Estonian and EU legal, policy and technical requirements. | Registry checks were treated as absolute conditions. An unavailable registry could block an application, personal identification code validation lacked checksum verification, and the system retained only pass/fail results. Most importantly, **"cannot verify" must not mean "not eligible."** | Identified the need to separate technical verification from eligibility decisions, introduce more robust personal identification code validation, and record meaningful verification evidence. |
| 22 October 2026 | 2.0 | Identified the legislation and legal provisions relevant to the proposed unemployment benefit registration process. | The applicable legal framework depends on the actual benefit scheme. The statutory subsistence benefit (*toimetulekutoetus*) is based on income and housing costs, rather than unemployment status alone. The precise benefit scheme and legal basis needed confirmation. | Identified the legal framework to inform system requirements, including GDPR, the Social Welfare Act, Population Register Act, Taxation Act, Labour Market Services and Benefits Act, Administrative Procedure Act and Public Information Act. |
| 22 October 2026 | 2.0 | Mapped relevant legal provisions to system requirements and corresponding design changes through compliance tables A–D. | Identified requirements concerning lawful data access, purpose limitation, data minimisation, transparency, decision justification, security and accountability. Some legal section references required further verification against the applicable legislation. | Translated legal obligations into proposed system requirements and design controls, providing a basis for revising the user story and acceptance criteria. |
| 24 October 2026 | 2 | Rewrote the acceptance criteria for SUPPORT-US-001 and prepared PowerPoint presentations to communicate the policy analysis, legal findings, compliance gaps and recommendations. | The original acceptance criteria did not sufficiently address evidence recording, auditability, reasoned decisions, correction or review mechanisms, and applicant-facing privacy information. | Developed 16 revised acceptance criteria covering distinct verification outcomes, structured decision reasons and legal bases, audit logging, retention, applicant information and safeguards against silent decision overwrites. Prepared presentations to communicate the proposed changes. |
| 25 October 2026 | 1.5 | Reviewed how to balance data minimisation with the need to retain sufficient evidence to explain administrative decisions. | Recording only `unemployment_check = pass` would not provide sufficient evidence to explain a disputed decision. Retaining complete registry responses, however, could create unnecessary privacy risks. | Proposed a minimal evidence record containing the check result, relevant status, source, retrieval timestamp and registry data timestamp, keeping retrieval time separate from the source-data timestamp. |
| 26 October 2026 | 1.5 | Reviewed and refined the policy analysis Word document before sharing. | Identified structural and editorial issues, including duplicate headings, an empty proposed verification model section, missing section numbering, incomplete content and drafting artefacts. | Refined the report structure, corrected headings and numbering, completed missing sections and clarified the status of the proposed user story and unresolved legal assumptions. |
| 24 November 2026 | 2.0 | Revisited the policy analysis to identify additional legal provisions and assess the proposed solution from a broader management and policy perspective. | Identified the need to consider additional legal requirements alongside institutional responsibilities, governance, accountability, implementation and the practical implications for public-sector service delivery. | Expanded the policy analysis beyond technical and legal compliance to include governance and management considerations that should inform system requirements and implementation decisions. |
| 25 November 2026 | 2.0 | Conducted a further review of the policy analysis, focusing on additional legal provisions and management and policy implications. | Reassessed the legal framework, institutional accountability, administrative decision-making, implementation responsibilities and alignment with broader public-sector policy objectives. | Further refined the policy analysis to connect legal obligations, administrative responsibilities and governance considerations with the proposed software solution and its implementation. |
| **Total** | **17.5** | | | |


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
