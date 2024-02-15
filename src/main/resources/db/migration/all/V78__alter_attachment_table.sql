ALTER TABLE `attachment`
ADD COLUMN `extension` VARCHAR(45) NULL AFTER `file`;

INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
  VALUES ('FILE-EXTENSION', 'File Extension', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('PDF', 'FILE-EXTENSION', 'pdf', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('JPEG', 'FILE-EXTENSION', 'jpeg', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
    VALUES ('JPG', 'FILE-EXTENSION', 'jpg', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
    VALUES ('PNG', 'FILE-EXTENSION', 'png', CURRENT_DATE, '9999-12-31');