INSERT INTO `number_range` (`current`, `end`, `object`, `start`, `valid_from`, `valid_to`)
VALUES ('1000000000', '1999999999', 'CASHUP', '1000000001', CURRENT_DATE, '9999-12-31');

INSERT INTO `number_range` (`current`, `end`, `object`, `start`, `valid_from`, `valid_to`)
VALUES ('1000000000', '1999999999', 'PAYMENT-REQUEST', '1000000001', CURRENT_DATE, '9999-12-31');

INSERT INTO `number_range` (`current`, `end`, `object`, `start`, `valid_from`, `valid_to`)
VALUES ('1000000000', '1999999999', 'DEPOSIT', '1000000001', CURRENT_DATE, '9999-12-31');

ALTER TABLE `transaction_amount`
ADD COLUMN `created_by` VARCHAR(45) NULL AFTER `amount`,
ADD COLUMN `changed_by` VARCHAR(45) NULL AFTER `created_by`;

CREATE TABLE `transaction_bank_account` (
  `transaction` VARCHAR(45) NOT NULL,
  `account_holder` VARCHAR(45) NULL,
  `bank_name` VARCHAR(45) NULL,
  `account_number` VARCHAR(45) NULL,
  `branch_code` VARCHAR(45) NULL,
  `account_type` VARCHAR(45) NULL,
  PRIMARY KEY (`transaction`));
