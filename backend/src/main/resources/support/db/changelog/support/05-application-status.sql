--liquibase formatted sql

--changeset ite4120:support-05-application-status
--comment Add the verification statuses
/* Revision 2: an application is OPEN until verified, PENDING_VERIFICATION
   while a required fact could not be verified, READY_FOR_DECISION when every
   required fact is verified, then APPROVED or REJECTED. */
alter table support.application drop constraint application_status_chk;

alter table support.application
    add constraint application_status_chk
    check (status in ('OPEN', 'PENDING_VERIFICATION', 'READY_FOR_DECISION', 'APPROVED', 'REJECTED'));
--
