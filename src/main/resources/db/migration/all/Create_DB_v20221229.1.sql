
CREATE DATABASE `mawa` /*!40100 DEFAULT CHARACTER SET utf8 */ /*!80016 DEFAULT ENCRYPTION='N' */;

CREATE TABLE `partner` (
  `id` varchar(20) NOT NULL,
  `birth_date` date DEFAULT NULL,
  `gender` varchar(20) DEFAULT NULL,
  `language` varchar(20) DEFAULT NULL,
  `marital_status` varchar(20) DEFAULT NULL,
  `name1` varchar(60) DEFAULT NULL,
  `name2` varchar(60) DEFAULT NULL,
  `name3` varchar(60) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `status_reason` varchar(20) DEFAULT NULL,
  `title` varchar(20) DEFAULT NULL,
  `type` varchar(20) DEFAULT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_to` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `transaction` (
  `id` varchar(200) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(45) DEFAULT NULL,
  `status_reason` varchar(45) DEFAULT NULL,
  `sub_type` varchar(45) DEFAULT NULL,
  `type` varchar(20) DEFAULT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_to` date DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `sub_status` varchar(45) DEFAULT NULL,
  `sub_description` longtext,
  `createdBy` varchar(45) DEFAULT NULL,
  `changedBy` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

