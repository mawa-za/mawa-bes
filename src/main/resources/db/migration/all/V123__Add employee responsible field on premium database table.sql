ALTER TABLE `membership_premium`
ADD COLUMN `employee_responsible` VARCHAR(45) NULL AFTER `amount`;

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('RELATION-TYPE', 'Relation Type', CURRENT_DATE, '9999-12-31');