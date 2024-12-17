DELIMITER $$
DROP VIEW IF EXISTS transaction_view $$

CREATE VIEW transaction_view AS
SELECT
		transaction.id AS 'transaction_id',
		transaction.type AS 'transaction_type',
		transaction.sub_type AS 'transaction_subtype',
		transaction.number AS 'transaction_number',
		partner_identity.type AS 'identity_type',
		partner_identity.value AS 'identity_number',
		CONCAT(main_partner.name1,' ', main_partner.name2, ' ', main_partner.name3) AS 'main_partner',
		CONCAT(employee.name1,' ', employee.name2, ' ', employee.name3) AS 'employee_responsible',
        CONCAT(created_by.name1, ' ', created_by.name2, ' ', created_by.name3) AS 'created_by',
        CONCAT(changed_by.name1, ' ', changed_by.name2, ' ', changed_by.name3) AS 'changed_by',
		product.description AS 'product',
		transaction.valid_from AS 'creation_date',
		transaction.category AS 'category',
		transaction.priority AS 'priority',
		transaction.status AS 'transaction_status',
        transaction_amount_collected.amount AS amount_collected,
        transaction_amount_deposited.amount AS amount_deposited,
		transaction_due_date.value AS 'due_date',
        transaction_death_date.value AS 'death_date',
        transaction_burial_date.value AS 'burial_date',
        transaction_date_joined.value AS 'date_joined',
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

LEFT JOIN transaction_amount AS transaction_amount_collected ON transaction.id = transaction_amount_collected.transaction
	AND (transaction_amount_collected.type = 'AMOUNT-COLLECTED')

LEFT JOIN transaction_amount AS transaction_amount_deposited ON transaction.id = transaction_amount_deposited.transaction
	AND (transaction_amount_deposited.type = 'AMOUNT-DEPOSITED')

LEFT JOIN transaction_date AS transaction_due_date ON transaction.id = transaction_due_date.transaction
	AND (transaction_due_date.type = 'DUE-DATE')

LEFT JOIN transaction_date AS transaction_death_date ON transaction.id = transaction_death_date.transaction
	AND (transaction_death_date.type = 'DEATH-DATE')

LEFT JOIN transaction_date AS transaction_burial_date ON transaction.id = transaction_burial_date.transaction
	AND (transaction_burial_date.type = 'BURIAL-DATE')

LEFT JOIN transaction_date AS transaction_date_joined ON transaction.id = transaction_date_joined.transaction
	AND (transaction_date_joined.type = 'DATE-JOINED')

LEFT JOIN transaction_link  ON transaction.id = transaction_link.transaction1
	AND (transaction.type = 'PAYMENT-REQUEST')

LEFT JOIN transaction_partner t_partner ON transaction.id = t_partner.transaction
	AND (t_partner.partner_function = 'CLAIMANT')

LEFT JOIN product ON product.id = item.product

LEFT JOIN partner_identity ON partner_identity.partner = main_tx_partner.partner

LEFT JOIN partner p ON t_partner.partner = p.id

LEFT JOIN user user_created_by ON transaction.created_by = user_created_by.username
LEFT JOIN partner AS created_by ON user_created_by.partner = created_by.id

LEFT JOIN user user_changed_by ON transaction.changed_by = user_changed_by.username
LEFT JOIN partner AS changed_by ON user_changed_by.partner = changed_by.id
$$

DELIMITER ;