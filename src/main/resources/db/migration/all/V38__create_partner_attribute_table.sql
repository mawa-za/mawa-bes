CREATE TABLE `partner_attribute` (
  `partner` VARCHAR(255) NOT NULL,
  `attribute` VARCHAR(45) NOT NULL,
  `value` VARCHAR(45) NULL,
  `valid_from` DATE NULL,
  `valid_to` DATE NULL,
  PRIMARY KEY (`partner`, `attribute`));