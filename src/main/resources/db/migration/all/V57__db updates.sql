INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('MEMBERSHIP-TYPE', 'Membership Type', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('INDIVIDUAL', 'MEMBERSHIP-TYPE', 'Individual', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('GROUP', 'MEMBERSHIP-TYPE', 'Group', CURRENT_DATE, '9999-12-31');

