INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('PRODUCT-CATEGORY', 'Product Category', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`,`valid_from`, `valid_to`)
VALUES ('SERVICE', 'PRODUCT-CATEGORY', 'Service',CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`,`valid_from`, `valid_to`)
VALUES ('CONSUMABLE', 'PRODUCT-CATEGORY', 'Consumable Products',CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`,`valid_from`, `valid_to`)
VALUES ('MEMBERSHIP', 'PRODUCT-CATEGORY', 'Membership',CURRENT_DATE, '9999-12-31');