ALTER TABLE `receipt`
ADD COLUMN `transaction_id` VARCHAR(45) NULL AFTER `receipt_type`;

