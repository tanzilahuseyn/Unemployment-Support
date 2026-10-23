--liquibase formatted sql

--changeset ite4120:support-91-demo-valid-codes context:demo
--comment Demo personal codes with valid checksums
/* Revision 2 validates the checksum. Three codes seeded by 90-demo-data.sql
   fail it, so they are corrected here; a released changeset is never edited. */
update support.application set applicant_isikukood = '49002020024' where applicant_isikukood = '49002020022';
update support.application set applicant_isikukood = '39003030035' where applicant_isikukood = '39003030033';
update support.application set applicant_isikukood = '49004040048' where applicant_isikukood = '49004040044';
--

--changeset ite4120:support-91-demo-more-cases context:demo
--comment Two more demo applications for revision 2 cases
/* 39005050059 is registered as unemployed and has an active employment, so
   the registries contradict each other (INCONCLUSIVE). For 49006060061 the
   imitated Unemployment Insurance Fund answers as unavailable, to show
   pending verification and manual verification. */
insert into support.application
    (applicant_isikukood, support_type_code, period_start, period_end, household_size)
values
    ('39005050059', 'MONTHLY', '2026-11-01', '2026-11-30', 2),
    ('49006060061', 'MONTHLY', '2026-11-01', '2026-12-31', 1);
--
