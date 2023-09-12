ALTER TABLE `partner_attribute`
CHANGE COLUMN `partner` `partner` VARCHAR(250) NOT NULL ,
CHANGE COLUMN `attribute` `attribute` VARCHAR(100) NOT NULL ,
CHANGE COLUMN `value` `value` VARCHAR(250) NULL DEFAULT NULL ;
