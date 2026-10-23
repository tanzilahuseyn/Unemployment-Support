--liquibase formatted sql

--changeset ite4120:support-07-decision-reasons
--comment Reasoned decisions with legal basis
/* Revision 2, Administrative Procedure Act section 56: a decision carries a
   structured reason code, the explanation (the existing reason column), the
   legal basis and rule version of the scheme, and how to challenge it. The
   override column now means an exception for a verified adverse fact. A
   check not required by the scheme is null, and INCONCLUSIVE is a result. */
alter table support.decision
    add column reason_code text,
    add column legal_basis text,
    add column rule_version text,
    add column review_info text;

/* Rejections made under revision 1 had only a free-text reason. They become
   OTHER, keeping that text as the explanation, so the rule below holds for
   every existing row. */
update support.decision set reason_code = 'OTHER' where outcome = 'REJECTED' and reason_code is null;

alter table support.decision
    add constraint decision_reason_code_chk
    check (reason_code in ('NOT_RESIDENT', 'NOT_REGISTERED_UNEMPLOYED', 'ACTIVE_EMPLOYMENT', 'INCOMPLETE_APPLICATION', 'OTHER'));

alter table support.decision
    add constraint decision_rejection_code_chk check (outcome <> 'REJECTED' or reason_code is not null);

alter table support.decision alter column residence_check drop not null;
alter table support.decision alter column unemployment_check drop not null;
alter table support.decision alter column employment_check drop not null;

alter table support.decision drop constraint decision_residence_chk;
alter table support.decision drop constraint decision_unemployment_chk;
alter table support.decision drop constraint decision_employment_chk;

alter table support.decision
    add constraint decision_residence_chk check (residence_check in ('PASS', 'FAIL', 'UNAVAILABLE', 'INCONCLUSIVE')),
    add constraint decision_unemployment_chk check (unemployment_check in ('PASS', 'FAIL', 'UNAVAILABLE', 'INCONCLUSIVE')),
    add constraint decision_employment_chk check (employment_check in ('PASS', 'FAIL', 'UNAVAILABLE', 'INCONCLUSIVE'));
--
