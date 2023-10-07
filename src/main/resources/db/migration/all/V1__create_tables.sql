
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `address` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `address_line_1` VARCHAR(45) NULL DEFAULT NULL,
  `address_line_2` VARCHAR(45) NULL DEFAULT NULL,
  `address_line_3` VARCHAR(45) NULL DEFAULT NULL,
  `address_line_4` VARCHAR(45) NULL DEFAULT NULL,
  `postal_code` VARCHAR(20) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


CREATE TABLE IF NOT EXISTS `attachment` (
  `id` VARCHAR(255) NOT NULL,
  `creation_by` DATETIME(6) NULL DEFAULT NULL,
  `creation_date` DATETIME(6) NULL DEFAULT NULL,
  `file_content` VARBINARY(255) NULL DEFAULT NULL,
  `file_name` VARCHAR(255) NULL DEFAULT NULL,
  `file_type` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


CREATE TABLE IF NOT EXISTS `employment` (
  `employee_id` VARCHAR(20) NOT NULL,
  `start_date` DATE NOT NULL,
  `branch` VARCHAR(255) NULL DEFAULT NULL,
  `department` VARCHAR(255) NULL DEFAULT NULL,
  `end_date` DATE NULL DEFAULT NULL,
  `position` VARCHAR(255) NULL DEFAULT NULL,
  `status` VARCHAR(255) NULL DEFAULT NULL,
  `type` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`employee_id`, `start_date`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


CREATE TABLE IF NOT EXISTS `field` (
  `code` VARCHAR(255) NOT NULL,
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `valid_from` VARCHAR(255) NULL DEFAULT NULL,
  `valid_to` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`code`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


CREATE TABLE IF NOT EXISTS `field_option` (
  `code` VARCHAR(20) NOT NULL,
  `field` VARCHAR(20) NOT NULL,
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`code`, `field`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

ALTER TABLE `field_option`
ADD COLUMN `type` VARCHAR(45) NOT NULL DEFAULT 'SYSTEM' AFTER `description`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`code`, `field`, `type`);

CREATE TABLE IF NOT EXISTS `number_range` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `current` VARCHAR(40) NULL DEFAULT NULL,
  `end` VARCHAR(40) NULL DEFAULT NULL,
  `object` VARCHAR(20) NULL DEFAULT NULL,
  `prefix` VARCHAR(20) NULL DEFAULT NULL,
  `start` VARCHAR(40) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `object_UNIQUE` (`object` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


CREATE TABLE IF NOT EXISTS `partner` (
  `id` VARCHAR(255) NOT NULL,
  `birth_date` DATE NULL DEFAULT NULL,
  `gender` VARCHAR(20) NULL DEFAULT NULL,
  `language` VARCHAR(20) NULL DEFAULT NULL,
  `marital_status` VARCHAR(20) NULL DEFAULT NULL,
  `name1` VARCHAR(60) NULL DEFAULT NULL,
  `name2` VARCHAR(60) NULL DEFAULT NULL,
  `name3` VARCHAR(60) NULL DEFAULT NULL,
  `no` VARCHAR(20) NULL DEFAULT NULL,
  `status` VARCHAR(20) NULL DEFAULT NULL,
  `status_reason` VARCHAR(20) NULL DEFAULT NULL,
  `title` VARCHAR(20) NULL DEFAULT NULL,
  `type` VARCHAR(20) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `no_UNIQUE` (`no` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

CREATE TABLE IF NOT EXISTS `partner_address` (
  `address_id` INT NOT NULL,
  `address_usage` VARCHAR(20) NOT NULL,
  `partner` VARCHAR(20) NOT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`address_id`, `address_usage`, `partner`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.partner_attachment
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `partner_attachment` (
  `id` VARCHAR(60) NOT NULL,
  `created_at` DATETIME(6) NULL DEFAULT NULL,
  `created_by` VARCHAR(200) NULL DEFAULT NULL,
  `extension` VARCHAR(20) NULL DEFAULT NULL,
  `name` VARCHAR(45) NULL DEFAULT NULL,
  `partner` VARCHAR(20) NULL DEFAULT NULL,
  `status` VARCHAR(45) NULL DEFAULT NULL,
  `status_reason` VARCHAR(45) NULL DEFAULT NULL,
  `type` VARCHAR(45) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.partner_bank_account
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `partner_bank_account` (
  `account_number` VARCHAR(255) NOT NULL,
  `partner` VARCHAR(255) NOT NULL,
  `type` VARCHAR(255) NOT NULL,
  `account_holder` VARCHAR(255) NULL DEFAULT NULL,
  `account_type` VARCHAR(255) NULL DEFAULT NULL,
  `bank_name` VARCHAR(255) NULL DEFAULT NULL,
  `branch_code` VARCHAR(255) NULL DEFAULT NULL,
  `branch_name` VARCHAR(255) NULL DEFAULT NULL,
  `status` VARCHAR(255) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`account_number`, `partner`, `type`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.partner_contact
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `partner_contact` (
  `partner` VARCHAR(20) NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  `value` VARCHAR(60) NULL DEFAULT NULL,
  PRIMARY KEY (`partner`, `type`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.partner_date
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `partner_date` (
  `partner_no` VARCHAR(20) NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `value` DATETIME(6) NULL DEFAULT NULL,
  PRIMARY KEY (`partner_no`, `type`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.partner_identity
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `partner_identity` (
  `type` VARCHAR(20) NOT NULL,
  `value` VARCHAR(60) NOT NULL,
  `partner` VARCHAR(20) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`type`, `value`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.partner_relation
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `partner_relation` (
  `partner1` VARCHAR(20) NOT NULL,
  `partner2` VARCHAR(20) NOT NULL,
  `type` VARCHAR(20) NOT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`partner1`, `partner2`, `type`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.partner_resources
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `partner_resources` (
  `resource_id` VARCHAR(20) NOT NULL,
  `partner_no` VARCHAR(45) NOT NULL,
  `partner_url` VARCHAR(20) NOT NULL,
  `port_number` VARCHAR(20) NOT NULL,
  `resource_name` VARCHAR(45) NULL DEFAULT NULL,
  `status` VARCHAR(20) NULL DEFAULT NULL,
  `status_reason` VARCHAR(20) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`resource_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.partner_role
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `partner_role` (
  `id` VARCHAR(20) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`, `role`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.product
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product` (
  `id` VARCHAR(60) NOT NULL,
  `code` VARCHAR(255) NULL DEFAULT NULL,
  `category` VARCHAR(20) NULL DEFAULT NULL,
  `description` VARCHAR(60) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `code_UNIQUE` (`code` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.product_pricing
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product_pricing` (
  `pricing` VARCHAR(255) NOT NULL,
  `product` VARCHAR(255) NOT NULL,
  `value` DECIMAL(38,2) NULL DEFAULT NULL,
  PRIMARY KEY (`pricing`, `product`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.role
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `role` (
  `id` VARCHAR(45) NOT NULL,
  `description` VARCHAR(60) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.role_workcenter
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `role_workcenter` (
  `role` VARCHAR(255) NOT NULL,
  `workcenter` VARCHAR(255) NOT NULL,
  `position` INT NULL DEFAULT NULL,
  PRIMARY KEY (`role`, `workcenter`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.settings
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `settings` (
  `attribute` VARCHAR(60) NOT NULL,
  `setting` VARCHAR(20) NOT NULL,
  `value` VARCHAR(60) NULL DEFAULT NULL,
  PRIMARY KEY (`attribute`, `setting`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.tenant
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tenant` (
  `id` VARCHAR(255) NOT NULL,
  `host` VARCHAR(255) NULL DEFAULT NULL,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  `status` VARCHAR(255) NULL DEFAULT NULL,
  `url` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.tenant_property
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tenant_property` (
  `property` VARCHAR(255) NOT NULL,
  `tenant` VARCHAR(255) NOT NULL,
  `value` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`property`, `tenant`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.transaction
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction` (
  `id` VARCHAR(255) NOT NULL,
  `no` VARCHAR(255) NULL DEFAULT NULL,
  `type` VARCHAR(255) NULL DEFAULT NULL,
  `changed_by` VARCHAR(255) NULL DEFAULT NULL,
  `created_by` VARCHAR(255) NULL DEFAULT NULL,
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `location` VARCHAR(255) NULL DEFAULT NULL,
  `status` VARCHAR(255) NULL DEFAULT NULL,
  `status_reason` VARCHAR(255) NULL DEFAULT NULL,
  `sub_description` TINYTEXT NULL DEFAULT NULL,
  `sub_status` VARCHAR(255) NULL DEFAULT NULL,
  `sub_type` VARCHAR(255) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  `number` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UniqueNumberAndType` (`no` ASC, `type` ASC) VISIBLE,
  UNIQUE INDEX `no_UNIQUE` (`no` ASC) VISIBLE,
  UNIQUE INDEX `type_UNIQUE` (`type` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.transaction_amount
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction_amount` (
  `transaction` VARCHAR(255) NOT NULL,
  `type` VARCHAR(255) NOT NULL,
  `amount` DECIMAL(38,2) NULL DEFAULT NULL,
  PRIMARY KEY (`transaction`, `type`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.transaction_date
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction_date` (
  `transaction` VARCHAR(255) NOT NULL,
  `type` VARCHAR(255) NOT NULL,
  `value` DATETIME(6) NULL DEFAULT NULL,
  PRIMARY KEY (`transaction`, `type`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.transaction_item
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction_item` (
  `item` VARCHAR(255) NOT NULL,
  `transaction` VARCHAR(255) NOT NULL,
  `product` VARCHAR(255) NULL DEFAULT NULL,
  `quantity` DECIMAL(38,2) NULL DEFAULT NULL,
  `unit_of_measure` VARCHAR(255) NULL DEFAULT NULL,
  `unit_price` DECIMAL(38,2) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`item`, `transaction`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.transaction_partner
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction_partner` (
  `partner_function` VARCHAR(255) NOT NULL,
  `partner` VARCHAR(255) NOT NULL,
  `transaction` VARCHAR(255) NOT NULL,
  `changed_by` VARCHAR(255) NULL DEFAULT NULL,
  `created_by` VARCHAR(255) NULL DEFAULT NULL,
  `date_added` DATE NULL DEFAULT NULL,
  `date_effective` DATE NULL DEFAULT NULL,
  `status` VARCHAR(255) NULL DEFAULT NULL,
  `status_reason` VARCHAR(255) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`partner_function`, `partner`, `transaction`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.user
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` VARCHAR(255) NOT NULL,
  `username` VARCHAR(100) NULL DEFAULT NULL,
  `cellphone` VARCHAR(20) NULL DEFAULT NULL,
  `email` VARCHAR(100) NULL DEFAULT NULL,
  `partner` VARCHAR(20) NULL DEFAULT NULL,
  `password` TINYBLOB NULL DEFAULT NULL,
  `password_status` VARCHAR(20) NULL DEFAULT NULL,
  `status` VARCHAR(20) NULL DEFAULT NULL,
  `status_reason` VARCHAR(45) NULL DEFAULT NULL,
  `user_type` VARCHAR(45) NULL DEFAULT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `username_UNIQUE` (`username` ASC) VISIBLE)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- ----------------------------------------------------------------------------
-- Table mawa.user_role
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_role` (
  `role` VARCHAR(255) NOT NULL,
  `user` VARCHAR(255) NOT NULL,
  `valid_from` DATE NULL DEFAULT NULL,
  `valid_to` DATE NULL DEFAULT NULL,
  PRIMARY KEY (`role`, `user`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;
SET FOREIGN_KEY_CHECKS = 1;
