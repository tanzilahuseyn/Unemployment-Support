--liquibase formatted sql

--changeset ite4120:support-92-demo-realistic-people context:demo
--comment Demo people with realistic birth dates
/* The demo personal codes encoded birth dates like 01.01.1990 and 02.02.1990,
   which read as obviously made up now that the list shows the date of birth.
   They are replaced with valid codes for varied, realistic birth dates. The
   people stay fictional, and each keeps the same case; a released changeset is
   never edited. */
update support.application set applicant_isikukood = '38703140274' where applicant_isikukood = '39001010011';
update support.application set applicant_isikukood = '49407225134' where applicant_isikukood = '49002020024';
update support.application set applicant_isikukood = '37911052485' where applicant_isikukood = '39003030035';
update support.application set applicant_isikukood = '48302283762' where applicant_isikukood = '49004040048';
update support.application set applicant_isikukood = '36809190424' where applicant_isikukood = '39005050059';
update support.application set applicant_isikukood = '60106117391' where applicant_isikukood = '49006060061';
--
