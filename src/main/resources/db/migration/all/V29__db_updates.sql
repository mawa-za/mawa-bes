ALTER TABLE `partner_address`
CHANGE COLUMN `partner` `partner` VARCHAR(255) NOT NULL ;

ALTER TABLE `partner_attachment`
CHANGE COLUMN `partner` `partner` VARCHAR(255) NULL DEFAULT NULL ;

ALTER TABLE `partner_contact`
CHANGE COLUMN `partner` `partner` VARCHAR(255) NOT NULL ;

ALTER TABLE `partner_date`
CHANGE COLUMN `partner_no` `partner_no` VARCHAR(255) NOT NULL ;

ALTER TABLE `partner_identity`
CHANGE COLUMN `partner` `partner` VARCHAR(255) NULL DEFAULT NULL ;

ALTER TABLE `partner_relation`
CHANGE COLUMN `partner1` `partner1` VARCHAR(255) NOT NULL ,
CHANGE COLUMN `partner2` `partner2` VARCHAR(255) NOT NULL ;

ALTER TABLE `partner_resources`
CHANGE COLUMN `partner_no` `partner_no` VARCHAR(255) NOT NULL ;

ALTER TABLE `partner_role`
CHANGE COLUMN `id` `partner` VARCHAR(255) NOT NULL ;

ALTER TABLE `employment`
CHANGE COLUMN `employee_id` `employee_id` VARCHAR(255) NOT NULL ;

