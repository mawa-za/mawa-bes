ALTER TABLE `product_pricing`
ADD COLUMN `valid_from` DATE NULL AFTER `value`,
ADD COLUMN `valid_to` DATE NULL AFTER `valid_from`;
