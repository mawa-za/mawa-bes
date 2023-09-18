CREATE TABLE IF NOT EXISTS `transaction_text` (
  `transaction` VARCHAR(255) NOT NULL,
  `type` VARCHAR(255) NOT NULL,
  `text` LONGTEXT NULL DEFAULT NULL,
  PRIMARY KEY (`transaction`, `type`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;