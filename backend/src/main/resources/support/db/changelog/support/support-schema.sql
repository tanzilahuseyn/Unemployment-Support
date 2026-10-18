--liquibase formatted sql

--changeset ite4120:support-schema
--comment Create the support schema
/* One schema per component, like animals. */
create schema if not exists support;
--
