DELIMITER $$
DROP VIEW IF EXISTS `transaction_view`$$
CREATE VIEW transaction_view AS
select
transaction.id as 'transaction_id',
transaction.type as 'transaction_type',
transaction.number as 'transaction_number',
partner_identity.type as 'identity_type',
partner_identity.value as 'identity_number',
customer.partner as 'customer',
member.partner as 'main_member',
employee.partner as 'employee_responsible',
item.product as 'product',
transaction.status as 'transaction_status'
from transaction
left join transaction_partner as customer on transaction.id = customer.transaction and customer.partner_function = 'CUSTOMER'
left join transaction_partner as member on transaction.id = member.transaction and member.partner_function = 'MAIN-MEMBER'
left join transaction_partner as employee on transaction.id = employee.transaction and (employee.partner_function = 'SALES-REPRESENTATIVE' OR employee.partner_function = 'EMPLOYEE-RESPONSIBLE' OR employee.partner_function = 'PERSON-RESPONSIBLE')
left join transaction_item as item on item.transaction =  transaction.id and transaction.type = 'MEMBERSHIP'
left join partner_identity on partner_identity.partner = customer.partner
$$

DELIMITER ;