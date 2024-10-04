DELIMITER $$

DROP FUNCTION IF EXISTS `getPartnerName`$$
CREATE FUNCTION getPartnerName(partnerIdIn VARCHAR(50))
   RETURNS VARCHAR(200)
   DETERMINISTIC
   BEGIN
	DECLARE partner_name VARCHAR(200);
	DECLARE lcl_name1 VARCHAR(200);
    DECLARE lcl_name2 varchar(200);
    DECLARE lcl_name3 varchar(200);
    SELECT name1, name2, name3 into lcl_name1, lcl_name2, lcl_name3 FROM partner where id = partnerIdIn LIMIT 1;
    SET partner_name = CONCAT(lcl_name1,' ', lcl_name2, ' ', lcl_name3);
	return partner_name;
   END$$

DROP VIEW IF EXISTS `transaction_view`$$
CREATE VIEW transaction_view AS
select
transaction.id as 'transaction_id',
transaction.type as 'transaction_type',
transaction.sub_type as 'transaction_subtype',
transaction.number as 'transaction_number',
partner_identity.type as 'identity_type',
partner_identity.value as 'identity_number',
CONCAT(main_partner.name1,' ', main_partner.name2, ' ', main_partner.name3) as 'main_partner',
CONCAT(employee.name1,' ', employee.name2, ' ', employee.name3) as 'employee_responsible',
product.description as 'product',
transaction.valid_from as 'creation_date',
transaction.category as 'category',
transaction.priority as 'priority',
transaction.status as 'transaction_status'
from transaction
left join transaction_partner as main_tx_partner on transaction.id = main_tx_partner.transaction and (main_tx_partner.partner_function = 'CUSTOMER' or main_tx_partner.partner_function = 'MAIN-MEMBER')
left join partner as main_partner on main_tx_partner.partner = main_partner.id
left join transaction_partner as employee_partner on transaction.id = employee_partner.transaction and (employee_partner.partner_function = 'SALES-REPRESENTATIVE' OR employee_partner.partner_function = 'EMPLOYEE-RESPONSIBLE' OR employee_partner.partner_function = 'PERSON-RESPONSIBLE')
left join partner as employee on employee_partner.partner = employee.id
left join transaction_item as item on item.transaction =  transaction.id and (transaction.type = 'MEMBERSHIP' or transaction.type = 'GROUP-SOCIETY')
left join product on product.id = item.product
left join partner_identity on partner_identity.partner = main_tx_partner.partner
$$

DELIMITER ;