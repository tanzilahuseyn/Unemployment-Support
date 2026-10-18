--liquibase formatted sql

--changeset ite4120:support-01-support-type
--comment Support type reference table
/* Natural-key reference data: no core.seq_id, no sys columns. */
create table support.support_type (
    code                text not null,
    name                text not null,
    constraint support_type_pk primary key (code)
);
--

--changeset ite4120:support-01-support-type-seed
--comment Seed the support types
/* The final list is open for the group (SUPPORT.01, Business tests).
   Convergent: ON CONFLICT DO NOTHING. */
insert into support.support_type (code, name) values
    ('ONE_OFF', 'One-off support'),
    ('MONTHLY', 'Monthly subsistence support')
on conflict do nothing;
--
