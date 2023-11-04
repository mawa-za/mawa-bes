INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('PAYMENT-REASON', 'Payment Reason', CURRENT_DATE, '9999-12-31');

ALTER TABLE `transaction`
ADD COLUMN `category` VARCHAR(45) NULL AFTER `number`;