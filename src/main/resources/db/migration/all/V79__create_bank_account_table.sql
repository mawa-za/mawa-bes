CREATE TABLE `bank_account` (
  `id` varchar(255) NOT NULL,
  `object_id` varchar(255) NOT NULL,
  `account_holder` varchar(255) DEFAULT NULL,
  `account_type` varchar(255) DEFAULT NULL,
  `account_number` varchar(255) NOT NULL,
  `bank_name` varchar(255) DEFAULT NULL,
  `branch_code` varchar(255) DEFAULT NULL,
  `branch_name` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_to` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) 