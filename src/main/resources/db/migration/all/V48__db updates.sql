INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('ADDRESS-TYPE', 'Address Type', CURRENT_DATE, '9999-12-31');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('CITY', 'City', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('RESIDENTIAL', 'ADDRESS-TYPE', 'Residential Address', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('POSTAL', 'ADDRESS-TYPE', 'Postal Address', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('WORK', 'ADDRESS-TYPE', 'Work Address', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('DELIVERY', 'ADDRESS-TYPE', 'Delivery Address', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('BILLING', 'ADDRESS-TYPE', 'Billing Address', CURRENT_DATE, '9999-12-31');