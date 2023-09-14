CREATE TABLE `product_attribute` (
  `product` VARCHAR(250) NOT NULL,
  `attribute` VARCHAR(100) NOT NULL,
  `value` VARCHAR(250) NULL,
  `valid_from` DATE NULL,
  `valid_to` DATE NULL,
  PRIMARY KEY (`product`, `attribute`));
