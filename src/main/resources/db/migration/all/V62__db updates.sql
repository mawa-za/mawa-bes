ALTER TABLE `receipt`
ADD COLUMN `location` VARCHAR(45) NULL AFTER `created_by`;

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
VALUES ('CARD', 'TENDER-TYPE', 'Card Payment', CURRENT_DATE, '9999-12-31');