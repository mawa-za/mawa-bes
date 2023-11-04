INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('CONTACT-TYPE', 'Contact Type', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('CELLPHONE', 'CONTACT-TYPE', 'Cellphone', CURRENT_DATE, '9999-12-31');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('TOWN', 'Town', CURRENT_DATE, '9999-12-31');
