
  INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
  VALUES ('TRANSACTION-AMOUNT', 'Transaction Amount', CURRENT_DATE, '9999-12-31');

  INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('AMOUNT-RECEIVED', 'TRANSACTION-AMOUNT', 'Amount Received', CURRENT_DATE, '9999-12-31');

  INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('AMOUNT-COLLECTED', 'TRANSACTION-AMOUNT', 'Amount Collected', CURRENT_DATE, '9999-12-31');

  INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('AMOUNT-DEPOSITED', 'TRANSACTION-AMOUNT', 'Amount Deposited', CURRENT_DATE, '9999-12-31');

  INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('MONTHLY-PREMIUM', 'TRANSACTION-AMOUNT', 'Monthly Premium', CURRENT_DATE, '9999-12-31');

  INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('AMOUNT-WITHDRAWN', 'TRANSACTION-AMOUNT', 'Amount Withdrawn', CURRENT_DATE, '9999-12-31');

  INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('AVAILABLE-BALANCE', 'TRANSACTION-AMOUNT', 'Available Balance', CURRENT_DATE, '9999-12-31');
