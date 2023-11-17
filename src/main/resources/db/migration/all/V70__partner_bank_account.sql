ALTER TABLE `mawa`.`partner_bank_account` 
DROP COLUMN `type`,
ADD COLUMN `id` VARCHAR(255) NULL FIRST,
CHANGE COLUMN `partner` `partner` VARCHAR(255) NULL AFTER `id`,
CHANGE COLUMN `account_number` `account_number` VARCHAR(255) NULL ,
DROP PRIMARY KEY;
;
