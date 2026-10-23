--liquibase formatted sql

--changeset ite4120:support-04-benefit-scheme
--comment Turn the support types into benefit schemes with their rules
/* Revision 2: each support type is a benefit scheme tied to its legal
   instrument, rule version, effective dates and the registry facts its
   rules require. Only the required registries are queried. */
alter table support.support_type
    add column legal_basis text,
    add column rule_version text,
    add column effective_from date,
    add column effective_to date,
    add column requires_residence boolean not null default true,
    add column requires_unemployment boolean not null default true,
    add column requires_no_employment boolean not null default true,
    add column review_info text;
--

--changeset ite4120:support-04-benefit-scheme-rules
--comment Placeholder rules for the two schemes
/* PLACEHOLDERS until the Tallinn instrument and its rules are confirmed
   (SUPPORT.01, Outside the code). The review information follows the
   Administrative Procedure Act: a challenge within 30 days. */
update support.support_type
   set legal_basis = 'Tallinn municipal social benefit regulation (placeholder until confirmed)',
       rule_version = '2026-1',
       effective_from = date '2026-01-01',
       requires_residence = true,
       requires_unemployment = true,
       requires_no_employment = false,
       review_info = 'You may file a challenge (vaie) with Mustamäe District Government within 30 days of receiving this decision, or appeal to the administrative court.'
 where code = 'ONE_OFF';

update support.support_type
   set legal_basis = 'Tallinn municipal social benefit regulation (placeholder until confirmed)',
       rule_version = '2026-1',
       effective_from = date '2026-01-01',
       requires_residence = true,
       requires_unemployment = true,
       requires_no_employment = true,
       review_info = 'You may file a challenge (vaie) with Mustamäe District Government within 30 days of receiving this decision, or appeal to the administrative court.'
 where code = 'MONTHLY';
--
