ALTER TABLE `partner`
ADD COLUMN `creation_date` DATE NULL AFTER `status_reason`,
ADD COLUMN `created_by` VARCHAR(45) NULL AFTER `creation_date`,
CHANGE COLUMN `type` `type` VARCHAR(20) NULL DEFAULT NULL AFTER `id`,
CHANGE COLUMN `no` `number` VARCHAR(20) NULL DEFAULT NULL AFTER `type`,
CHANGE COLUMN `title` `title` VARCHAR(20) NULL DEFAULT NULL AFTER `number`;
