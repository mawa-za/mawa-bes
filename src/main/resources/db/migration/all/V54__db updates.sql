ALTER TABLE `address`
CHANGE COLUMN `id` `id` VARCHAR(255) NOT NULL ;

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('GROUP', 'PARTNER-TYPE', 'Group', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('PROSPECT', 'PARTNER-ROLE', 'Prospect', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('COMPETITOR', 'PARTNER-ROLE', 'Competitor', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('SUPPLIER', 'PARTNER-ROLE', 'Supplier', CURRENT_DATE, '9999-12-31');