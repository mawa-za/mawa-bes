DELETE FROM `field`
WHERE `code` IN ('CASE', 'CASE-TYPE');

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
VALUES ('CASE-TYPE', 'Case Type', CURRENT_DATE, '9999-12-31');