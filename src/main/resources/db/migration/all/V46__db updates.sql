CREATE TABLE `notification` (
  `id` VARCHAR(255) NOT NULL,
  `type` VARCHAR(45) NULL,
  `sender` VARCHAR(200) NULL,
  `recipient` VARCHAR(200) NULL,
  `subject` TEXT NULL,
  `body` LONGTEXT NULL,
  `created_by` VARCHAR(45) NULL,
  `created_at` TIMESTAMP NULL,
  `status` VARCHAR(45) NULL,
  PRIMARY KEY (`id`));

CREATE TABLE `notification_log` (
  `log_id` INT NOT NULL AUTO_INCREMENT,
  `notification_id` VARCHAR(255) NOT NULL,
  `action` VARCHAR(45) NULL,
  `result` VARCHAR(45) NULL,
  `executed_at` TIMESTAMP NULL,
  PRIMARY KEY (`log_id`));
