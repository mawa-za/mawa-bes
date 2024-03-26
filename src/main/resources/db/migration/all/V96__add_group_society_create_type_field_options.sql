INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
  VALUES ('GROUP-SOCIETY-CREATION-TYPE', 'Group Society Creation Type', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('NEW', 'GROUP-SOCIETY-CREATION-TYPE', 'New', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('EXISTING', 'GROUP-SOCIETY-CREATION-TYPE', 'Existing', CURRENT_DATE, '9999-12-31');

