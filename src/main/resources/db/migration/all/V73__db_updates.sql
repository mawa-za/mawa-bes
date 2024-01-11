ALTER TABLE `employment`
ADD COLUMN `id` VARCHAR(255) NOT NULL FIRST,
ADD COLUMN `employee_number` VARCHAR(255) NULL AFTER `partner_id`,
CHANGE COLUMN `start_date` `start_date` DATE NULL AFTER `department`,
CHANGE COLUMN `employee_id` `partner_id` VARCHAR(255) NULL ,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`);
;


INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('DISPUTED', 'TRANSACTION-STATUS', 'Disputed', CURRENT_DATE, '9999-12-31');