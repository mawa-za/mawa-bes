ALTER TABLE `transaction_attribute`
ADD COLUMN `id` VARCHAR(255) NOT NULL FIRST,
CHANGE COLUMN `transaction` `transaction` VARCHAR(255) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_0900_ai_ci' NULL ,
CHANGE COLUMN `attribute` `attribute` VARCHAR(45) CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_0900_ai_ci' NULL ,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`);
;
