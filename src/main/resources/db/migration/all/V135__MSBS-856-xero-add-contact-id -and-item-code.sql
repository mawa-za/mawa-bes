INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES	('XER0-CONTACT-ID', 'ID-TYPE', 'Xero contact id', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES	('XERO-ITEM-CODE', 'PRODUCT-ATTRIBUTE', 'Xero item code', CURRENT_DATE, '9999-12-31');

INSERT INTO `settings` (`attribute`, `setting`, `value`)
VALUES	('XERO-INTEGRATION', 'INVOICE', 0 );