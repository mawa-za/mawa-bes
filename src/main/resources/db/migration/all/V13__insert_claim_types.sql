DELETE FROM `field_option` WHERE (`code` = 'SERVICE') and (`field` = 'CLAIM-TYPE');

INSERT INTO `field_option` (`code`, `field`, `description`,`valid_from`, `valid_to`)
VALUES ('FUNERAL', 'CLAIM-TYPE', 'Funeral',CURRENT_DATE, '9999-12-31');
