ALTER TABLE `partner_bank_account`
CHANGE COLUMN `type` `id` VARCHAR(255) NOT NULL FIRST,
CHANGE COLUMN `account_number` `account_number` VARCHAR(255) NULL AFTER `account_holder`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`);
;