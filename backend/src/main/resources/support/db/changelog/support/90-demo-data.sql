--liquibase formatted sql

--changeset ite4120:support-90-demo-applications context:demo
--comment Demo applications for local development
/* One open application for each fictional person the imitated registries
   know (MockSupportRegistriesController), so every check result can be seen:
   39001010011 passes all three checks, 49002020022 lives in another
   municipality, 39003030033 has an active employment, and 49004040044 is
   not registered as unemployed. */
insert into support.application
    (applicant_isikukood, support_type_code, period_start, period_end, household_size)
values
    ('39001010011', 'MONTHLY', '2026-10-01', '2026-10-31', 1),
    ('49002020022', 'MONTHLY', '2026-10-01', '2026-12-31', 3),
    ('39003030033', 'ONE_OFF', '2026-10-15', '2026-10-15', 2),
    ('49004040044', 'MONTHLY', '2026-11-01', '2026-11-30', 4);
--
