INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('GROUP-FUNERAL', 'CLAIM-TYPE', 'Group Funeral', CURRENT_DATE, '9999-12-31');

DELETE FROM `field_option` WHERE (`code` = 'GROUP-SOCIETY-FUNERAL') and (`field` = 'CLAIM-TYPE') and (`type` = 'SYSTEM');
