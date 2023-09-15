INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('MEMBERSHIP-CREATION-TYPE', 'Membership Creation Type', '2023-09-14', '9999-12-31');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('UOM', 'Unit Of Measure', '2023-09-14', '9999-12-31');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('USER-TYPE', 'User Type', '2023-09-14', '9999-12-31');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('PAYMENT-METHOD', 'Payment Type', '2023-09-14', '9999-12-31');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('TENDER-TYPE', 'Tender Type', '2023-09-14', '9999-12-31');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('SUBURB', 'Suburb', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('NEW', 'MEMBERSHIP-CREATION-TYPE', 'New', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('TRANSFER', 'MEMBERSHIP-CREATION-TYPE', 'Transfer', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('MIDRAND', 'SUBURB', 'Midrand', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('EA', 'UOM', 'Each', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('DEVELOPER', 'USER-TYPE', 'Developer', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('EXTERNAL', 'USER-TYPE', 'External', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('GUEST', 'USER-TYPE', 'Guest', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('INTERNAL', 'USER-TYPE', 'Internal', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('ADMIN', 'USER-TYPE', 'Admin', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('CASH', 'PAYMENT-METHOD', 'Cash', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('EFT', 'PAYMENT-METHOD', 'Electronic Funds Transfer', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('CASH', 'TENDER-TYPE', 'Cash', '2023-09-14', '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('EFT', 'TENDER-TYPE', 'Electronic Funds Transfer', '2023-09-14', '9999-12-31');