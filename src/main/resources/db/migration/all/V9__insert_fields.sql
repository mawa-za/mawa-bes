ALTER TABLE `mawa`.`field`
DROP COLUMN `id`,
CHANGE COLUMN `code` `code` VARCHAR(255) NOT NULL ,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`code`),
DROP INDEX `field_code_UNIQUE` ;
;

INSERT INTO `mawa`.`field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('TEXT-TYPE', 'Text Type', CURRENT_DATE, '9999-12-31');

INSERT INTO `mawa`.`field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('CLAIM-DECLINE-REASON', 'Claim Decline Reasons', CURRENT_DATE, '9999-12-31');

INSERT INTO `mawa`.`field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('CLAIM-TYPE', 'Claim Type', CURRENT_DATE, '9999-12-31');

INSERT INTO `mawa`.`field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('GENDER', 'Gender', CURRENT_DATE, '9999-12-31');

INSERT INTO `mawa`.`field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('SALES-AREA', 'Sales Area', CURRENT_DATE, '9999-12-31');

INSERT INTO `mawa`.`field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('ID-TYPE', 'Identity Type', CURRENT_DATE, '9999-12-31');
