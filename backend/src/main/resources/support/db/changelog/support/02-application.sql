--liquibase formatted sql

--changeset ite4120:support-02-application
--comment Applications for unemployment support
/* Privacy: only the personal code identifies the applicant. Name, address
   and every registry answer are looked up live and never stored here.
   applicant_isikukood is TEXT, an identifier, never a number. "One open
   application per person for overlapping periods" needs a range comparison,
   so the service enforces it; the database holds the single-row rules. */
create table support.application (
    id                  bigint default nextval('core.seq_id') not null,
    applicant_isikukood text not null,
    support_type_code   text not null,
    period_start        date not null,
    period_end          date not null,
    household_size      int not null,
    status              text not null default 'OPEN',
    sys_status          char(1),
    sys_version         int,
    sys_created_at      timestamptz,
    sys_created_by      text,
    sys_modified_at     timestamptz,
    sys_modified_by     text,
    constraint application_pk primary key (id),
    constraint application_type_fk foreign key (support_type_code) references support.support_type (code),
    constraint application_isikukood_chk check (applicant_isikukood ~ '^[0-9]{11}$'),
    constraint application_period_chk check (period_end >= period_start),
    constraint application_household_chk check (household_size between 1 and 20),
    constraint application_status_chk check (status in ('OPEN', 'APPROVED', 'REJECTED'))
);

select core.create_table_metadata('support.application');

create index application_isikukood_idx
    on support.application (applicant_isikukood) where (sys_status = 'A');

create index application_status_idx on support.application (status) where (sys_status = 'A');
--
