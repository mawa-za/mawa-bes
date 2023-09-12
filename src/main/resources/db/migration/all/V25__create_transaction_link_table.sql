CREATE TABLE `transaction_link` (
  `transaction1` VARCHAR(45) NOT NULL,
  `transaction2` VARCHAR(45) NOT NULL,
  `type` VARCHAR(45) NOT NULL,
  `creation_date` DATE NULL,
  `created_by` VARCHAR(45) NULL,
  PRIMARY KEY (`transaction1`, `type`, `transaction2`));
