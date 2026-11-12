--liquibase formatted sql

--changeset ite4120:support-09-drop-animal-example
--comment Drop the course template's animal example schema
/* The animal register was the template's worked example. This repository now
   holds only the unemployment support component, so its schema is removed from
   any database that still has it. A fresh database never had it: nothing happens. */
drop schema if exists animals cascade;
--
