CREATE TABLE `transaction_attachment` (
  `transaction` VARCHAR(255) NOT NULL,
  `file_id` VARCHAR(255) NULL,
  `file_type` VARCHAR(45) NOT NULL,
  `valid_from` DATE NULL,
  `valid_to` DATE NULL,
  `status` VARCHAR(45) NULL,
  PRIMARY KEY (`transaction`, `file_type`));

  ALTER TABLE `partner_attachment`
  DROP COLUMN `status_reason`,
  DROP COLUMN `name`,
  DROP COLUMN `extension`,
  DROP COLUMN `created_by`,
  DROP COLUMN `created_at`,
  CHANGE COLUMN `id` `file_id` VARCHAR(255) NULL AFTER `file_type`,
  CHANGE COLUMN `status` `status` VARCHAR(45) NULL DEFAULT NULL AFTER `valid_to`,
  CHANGE COLUMN `partner` `partner` VARCHAR(255) NOT NULL ,
  CHANGE COLUMN `type` `file_type` VARCHAR(45) NOT NULL ,
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (`partner`, `file_type`);
  ;
