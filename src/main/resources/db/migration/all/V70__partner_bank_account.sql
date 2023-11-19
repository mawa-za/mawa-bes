DROP TABLE `partner_bank_account`;

CREATE TABLE IF NOT EXISTS `partner_bank_account` (
  `id` VARCHAR(255) NOT NULL,
  `partner` VARCHAR(255) NOT NULL,
  `account_holder` VARCHAR(255) NULL DEFAULT NULL,
  `account_type` VARCHAR(255) NULL DEFAULT NULL,
  `account_number` VARCHAR(255) NOT NULL,
  `bank_name` VARCHAR(255) NULL DEFAULT NULL,
  `branch_code` VARCHAR(255) NULL DEFAULT NULL,
  `branch_name` VARCHAR(255) NULL DEFAULT NULL,
  `status` VARCHAR(255) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;