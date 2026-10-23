--liquibase formatted sql

--changeset ite4120:support-08-audit-event
--comment Append-only audit log
/* Who did what, when, to which application: searches by personal code,
   views, changes, registry queries, manual verifications and decisions.
   Detail is a short text, never a registry payload, name or address. A log,
   not a business record: no sys columns, and rows are never changed. */
create table support.audit_event (
    id                  bigint default nextval('core.seq_id') not null,
    occurred_at         timestamptz not null default now(),
    actor               text not null,
    action              text not null,
    application_id      bigint,
    detail              text,
    constraint audit_event_pk primary key (id),
    constraint audit_event_action_chk check (action in ('SEARCH', 'VIEW', 'CREATE', 'UPDATE', 'WITHDRAW', 'REGISTRY_QUERY', 'MANUAL_VERIFICATION', 'DECISION')),
    constraint audit_event_detail_chk check (length(detail) <= 500)
);

create index audit_event_application_idx on support.audit_event (application_id);
--

--changeset ite4120:support-08-audit-append-only splitStatements:false
--comment Refuse updates and deletes on the audit log
/* Tamper resistance: a trigger refuses every UPDATE and DELETE. The function
   body is PL/pgSQL, so this changeset is not split on semicolons. */
create function support.audit_event_append_only() returns trigger
language plpgsql as $$
begin
    raise exception 'support.audit_event is append-only';
end
$$;

create trigger audit_event_append_only
    before update or delete on support.audit_event
    for each row execute function support.audit_event_append_only();
--
