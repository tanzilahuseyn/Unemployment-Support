--liquibase formatted sql

--changeset ite4120:support-06-check-evidence
--comment One evidence record per registry check
/* A minimal, purpose-specific snapshot: result, a short status, the source,
   when it was retrieved and how current the registry data was. Never names,
   addresses or full registry answers. The latest row per application and
   check type is current; older rows stay as history. decision_id marks the
   evidence a decision relied on. */
create table support.check_evidence (
    id                  bigint default nextval('core.seq_id') not null,
    application_id      bigint not null,
    check_type          text not null,
    result              text not null,
    status_detail       text,
    source              text not null,
    retrieved_at        timestamptz not null,
    source_data_at      timestamptz,
    manual              boolean not null default false,
    note                text,
    decision_id         bigint,
    sys_status          char(1),
    sys_version         int,
    sys_created_at      timestamptz,
    sys_created_by      text,
    sys_modified_at     timestamptz,
    sys_modified_by     text,
    constraint check_evidence_pk primary key (id),
    constraint check_evidence_application_fk foreign key (application_id) references support.application (id),
    constraint check_evidence_decision_fk foreign key (decision_id) references support.decision (id),
    constraint check_evidence_type_chk check (check_type in ('RESIDENCE', 'UNEMPLOYMENT', 'EMPLOYMENT')),
    constraint check_evidence_result_chk check (result in ('PASS', 'FAIL', 'UNAVAILABLE', 'INCONCLUSIVE')),
    constraint check_evidence_detail_chk check (length(status_detail) <= 100),
    constraint check_evidence_note_chk check (length(note) <= 1000),
    constraint check_evidence_manual_chk check (not manual or (length(trim(note)) > 0 and result in ('PASS', 'FAIL')))
);

select core.create_table_metadata('support.check_evidence');

create index check_evidence_application_idx
    on support.check_evidence (application_id, check_type) where (sys_status = 'A');
--
