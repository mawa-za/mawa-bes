CREATE TABLE `membership_premium` (
  `id` VARCHAR(50) NOT NULL,
  `number` VARCHAR(45) NULL,
  `creation_date` DATETIME NULL,
  `creation_time` DATETIME NULL,
  `created_by` VARCHAR(45) NULL,
  `membership_id` VARCHAR(45) NULL,
  `location` VARCHAR(45) NULL,
  `membership_period` VARCHAR(45) NULL,
  `tender_type` VARCHAR(45) NULL,
  `amount` DECIMAL(38,2) NULL,
  `terminal_id` VARCHAR(100) NULL,
  PRIMARY KEY (`id`));
