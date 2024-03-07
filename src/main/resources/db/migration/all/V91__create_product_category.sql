CREATE TABLE `product_category` (
  `product` varchar(255) NOT NULL,
  `category` varchar(255) NOT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_to` date DEFAULT NULL,
  PRIMARY KEY (`product`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
