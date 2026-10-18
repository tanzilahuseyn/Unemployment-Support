--liquibase formatted sql

--changeset ite4120:support-03-decision
--comment One decision per application
/* Who decided and when are the platform columns sys_created_by and
   sys_created_at. The three checks are stored only as their outcome at the
   moment of deciding (PASS, FAIL or UNAVAILABLE), never the registries'
   answers. A reason is required when rejecting and when overriding, and
   an override exists only on an approval. */
create table support.decision (
    id                  bigint default nextval('core.seq_id') not null,
    application_id      bigint not null,
    outcome             text not null,
    override            boolean not null default false,
    reason              text,
    residence_check     text not null,
    unemployment_check  text not null,
    employment_check    text not null,
    sys_status          char(1),
    sys_version         int,
    sys_created_at      timestamptz,
    sys_created_by      text,
    sys_modified_at     timestamptz,
    sys_modified_by     text,
    constraint decision_pk primary key (id),
    constraint decision_application_fk foreign key (application_id) references support.application (id),
    constraint decision_outcome_chk check (outcome in ('APPROVED', 'REJECTED')),
    constraint decision_override_chk check (not override or outcome = 'APPROVED'),
    constraint decision_reason_chk check (not (outcome = 'REJECTED' or override) or length(trim(reason)) > 0),
    constraint decision_reason_length_chk check (length(reason) <= 2000),
    constraint decision_residence_chk check (residence_check in ('PASS', 'FAIL', 'UNAVAILABLE')),
    constraint decision_unemployment_chk check (unemployment_check in ('PASS', 'FAIL', 'UNAVAILABLE')),
    constraint decision_employment_chk check (employment_check in ('PASS', 'FAIL', 'UNAVAILABLE'))
);

select core.create_table_metadata('support.decision');

/* One decision per application. */
create unique index decision_application_ukey
    on support.decision (application_id) where (sys_status = 'A');
--
