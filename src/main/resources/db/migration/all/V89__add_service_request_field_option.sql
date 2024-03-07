INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
  VALUES ('SERVICE-REQUEST-STATUS-REASON', 'Service Request Status Reason', CURRENT_DATE, '9999-12-31');

ALTER TABLE `transaction` ADD COLUMN `priority` VARCHAR(45) NULL AFTER `category`;