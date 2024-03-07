CREATE TABLE `product_type_category` (
  `type` varchar(255) NOT NULL,
  `category` varchar(255) NOT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_to` date DEFAULT NULL,
  PRIMARY KEY (`type`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
