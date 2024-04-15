ALTER TABLE `membership_premium`
ADD COLUMN `ext_receipt_number` VARCHAR(45) NULL AFTER `number`,
ADD UNIQUE INDEX `premium_ext_receipt_number_UNIQUE` (`ext_receipt_number` ASC) VISIBLE;
;

ALTER TABLE `receipt`
ADD COLUMN `ext_receipt_number` VARCHAR(45) NULL AFTER `receipt_number`,
ADD UNIQUE INDEX `receipt_ext_receipt_number_UNIQUE` (`ext_receipt_number` ASC) VISIBLE;
;
