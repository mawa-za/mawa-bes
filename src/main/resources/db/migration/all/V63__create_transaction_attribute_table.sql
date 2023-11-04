CREATE TABLE `transaction_attribute` (
  `transaction` VARCHAR(255) NOT NULL,
  `attribute` VARCHAR(45) NOT NULL,
  `value` VARCHAR(45) NULL,
  `valid_from` DATE NULL,
  `valid_to` DATE NULL,
  PRIMARY KEY (`transaction`, `attribute`));

  INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
  VALUES ('TRANSACTION-ATTRIBUTE', 'Transaction Attribute', CURRENT_DATE, '9999-12-31');

  INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('LAST-PREMIUM-PERIOD', 'TRANSACTION-ATTRIBUTE', 'Last Premium Period', CURRENT_DATE, '9999-12-31');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('CLAIM-DISPUTE-REASON', 'Claim Dispute Reason', CURRENT_DATE, '9999-12-31');