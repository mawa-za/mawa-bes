DELETE FROM `field`
WHERE `code` IN ('CALENDAR-PRODUCT-CATEGORY');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('CALENDAR-PRODUCT-CATEGORY', 'Calendar product category', CURRENT_DATE, '9999-12-31');