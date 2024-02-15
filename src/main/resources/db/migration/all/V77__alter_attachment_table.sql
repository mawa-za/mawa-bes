ALTER TABLE `attachment`
ADD COLUMN `object_id` VARCHAR(255) NULL AFTER `id`,
ADD COLUMN `document_type` VARCHAR(45) NULL AFTER `object_id`;

