INSERT INTO `field` (`code`, `description`, `valid_from`, `valid_to`)
  VALUES ('PRODUCT-PRICING', 'Product Pricing', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('COST-PRICE', 'PRODUCT-PRICING', 'Cost Price', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('SELLING-PRICE', 'PRODUCT-PRICING', 'Selling Price', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
  VALUES ('MONTHLY-PREMIUM', 'PRODUCT-PRICING', 'Monthly Premium', CURRENT_DATE, '9999-12-31');

INSERT INTO `field_option` (`code`, `field`, `description`, `valid_from`, `valid_to`)
    VALUES ('PREMIUM', 'PRODUCT-PRICING', 'Premium', CURRENT_DATE, '9999-12-31');
