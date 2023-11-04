ALTER TABLE `transaction_attachment`
CHANGE COLUMN `file_type` `document_type` VARCHAR(45) NOT NULL ;

ALTER TABLE `partner_attachment`
CHANGE COLUMN `file_type` `document_type` VARCHAR(45) NOT NULL ;

ALTER TABLE `attachment`
CHANGE COLUMN `downnload_by` `download_by` VARCHAR(45) NULL DEFAULT NULL ;

