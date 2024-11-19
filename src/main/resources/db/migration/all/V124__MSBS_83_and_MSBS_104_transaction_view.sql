DELIMITER $$
DROP VIEW IF EXISTS transaction_view $$

CREATE VIEW transaction_view AS
SELECT
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
		transaction.status as 'transaction_status',
		transaction_due_date.value AS 'due_date',
        transaction_death_date.value AS 'death_date',
        transaction_burial_date.value AS 'burial_date',
        transaction_link.transaction2 AS 'reference',
        CONCAT( p.name1, ' ', p.name2, ' ', p.name3) AS 'claimant'

FROM transaction
LEFT JOIN transaction_partner AS main_tx_partner ON transaction.id = main_tx_partner.transaction
	AND (main_tx_partner.partner_function = 'CUSTOMER' OR main_tx_partner.partner_function = 'MAIN-MEMBER')

LEFT JOIN partner AS main_partner ON main_tx_partner.partner = main_partner.id

LEFT JOIN transaction_partner AS employee_partner ON transaction.id = employee_partner.transaction
	AND (employee_partner.partner_function = 'SALES-REPRESENTATIVE' OR employee_partner.partner_function = 'EMPLOYEE-RESPONSIBLE' OR employee_partner.partner_function = 'PERSON-RESPONSIBLE')

LEFT JOIN partner AS employee ON employee_partner.partner = employee.id

LEFT JOIN transaction_item AS item ON item.transaction =  transaction.id
	AND (transaction.type = 'MEMBERSHIP' OR transaction.type = 'GROUP-SOCIETY')

LEFT JOIN transaction_date AS transaction_due_date ON transaction.id = transaction_due_date.transaction
	AND (transaction_due_date.type = 'DUE-DATE')

LEFT JOIN transaction_date AS transaction_death_date ON transaction.id = transaction_death_date.transaction
	AND (transaction_death_date.type = 'DEATH-DATE')

LEFT JOIN transaction_date AS transaction_burial_date ON transaction.id = transaction_burial_date.transaction
	AND (transaction_burial_date.type = 'BURIAL-DATE')

LEFT JOIN transaction_link  ON transaction.id = transaction_link.transaction1
	AND transaction.type = 'PAYMENT-REQUEST'

LEFT JOIN transaction_partner t_partner ON transaction.id = t_partner.transaction
	AND (t_partner.partner_function = 'CLAIMANT')

LEFT JOIN product ON product.id = item.product

LEFT JOIN partner_identity ON partner_identity.partner = main_tx_partner.partner

LEFT JOIN partner p ON t_partner.partner = p.id
$$

DELIMITER ;