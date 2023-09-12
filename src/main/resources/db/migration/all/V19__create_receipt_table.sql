CREATE TABLE `receipt` (
  `id` VARCHAR(50) NOT NULL,
  `receipt_number` VARCHAR(45) NULL,
  `receipt_type` VARCHAR(45) NULL,
  `creation_date` DATETIME NULL,
  `creation_time` DATETIME NULL,
  `created_by` VARCHAR(45) NULL,
  `invoice_number` VARCHAR(45) NULL,
  `membership_number` VARCHAR(45) NULL,
  `membership_period` VARCHAR(45) NULL,
  `tender_type` VARCHAR(45) NULL,
  `amount` DECIMAL(38,2) NULL,
  PRIMARY KEY (`id`));
