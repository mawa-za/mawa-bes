DROP TABLE `transaction_amount`;

CREATE TABLE `transaction_amount` (
  `id` VARCHAR(255) NOT NULL,
  `transaction` VARCHAR(255) NULL,
  `type` VARCHAR(45) NULL,
  `amount` DECIMAL(38,2) NULL,
  `created_by` VARCHAR(45) NULL,
  `changed_by` VARCHAR(45) NULL,
  PRIMARY KEY (`id`));

