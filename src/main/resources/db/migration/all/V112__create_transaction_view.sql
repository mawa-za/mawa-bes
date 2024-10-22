DELIMITER $$
DROP FUNCTION IF EXISTS `getPartnerSAIDNumber`$$
CREATE FUNCTION getPartnerSAIDNumber(partnerIdIn VARCHAR(50))
   RETURNS VARCHAR(200)
   DETERMINISTIC
   BEGIN
	DECLARE lcl_identity_no VARCHAR(200);
    SELECT value into lcl_identity_no FROM partner_identity where partner = partnerIdIn AND type = 'SA-ID' LIMIT 1;
	return lcl_identity_no;
   END$$

DROP FUNCTION IF EXISTS `getFieldOptionDesc`$$
CREATE FUNCTION getFieldOptionDesc(fieldIn VARCHAR(50),optionIn VARCHAR(20))
   RETURNS VARCHAR(200)
   DETERMINISTIC
   BEGIN
	DECLARE lcl_description VARCHAR(200);
    SELECT description into lcl_description FROM field_option where field = fieldIn and code = optionIn LIMIT 1;
	return lcl_description;
   END$$

DROP FUNCTION IF EXISTS `getPatnerName`$$
CREATE FUNCTION getPatnerName(partnerIdIn VARCHAR(50))
   RETURNS VARCHAR(200)
   DETERMINISTIC
   BEGIN
	DECLARE partner_name VARCHAR(200);
    DECLARE lcl_title varchar(20);
	DECLARE lcl_name1 VARCHAR(200);
    DECLARE lcl_name2 varchar(200);
    DECLARE lcl_name3 varchar(200);
    SELECT getFieldOptionDesc('TITLE', title), name1, name2, name3 into lcl_title, lcl_name1, lcl_name2, lcl_name3 FROM partner where id = partnerIdIn LIMIT 1;
    SET partner_name = CONCAT(lcl_title,' ',lcl_name1,' ', lcl_name2, ' ', lcl_name3);
	return partner_name;
   END$$

DROP VIEW IF EXISTS `transaction_view`$$
CREATE VIEW transaction_view AS
select
transaction_id,transacton_number,transaction_type, creation_date, transaction_type_desc, transaction_status, customer_sa_id, customer_name, employee_responsible,
CONCAT(IFNULL(transacton_number, ""),IFNULL(transaction_type, ""),IFNULL(creation_date, ""),IFNULL(transaction_type_desc, ""),IFNULL(transaction_status, ""),IFNULL(customer_sa_id, ""),IFNULL(customer_name, ""),IFNULL(employee_responsible, "")) as 'filter'
from
(SELECT
id as 'transaction_id',
number as 'transacton_number',
type as 'transaction_type',
valid_from as 'creation_date',
getFieldOptionDesc('TRANSACTION-TYPE', type) AS 'transaction_type_desc',
getFieldOptionDesc('TRANSACTION-STATUS', status) AS 'transaction_status',
getPartnerSAIDNumber((select partner from transaction_partner where (partner_function = 'CUSTOMER' OR partner_function = 'MAIN-MEMBER') AND transaction = transaction.id)) as customer_sa_id,
getPatnerName((select partner from transaction_partner where (partner_function = 'CUSTOMER' OR partner_function = 'MAIN-MEMBER') AND transaction = transaction.id)) as 'customer_name',
getPatnerName((select partner from transaction_partner where (partner_function = 'SALES-REPRESENTATIVE' OR partner_function = 'EMPLOYEE-RESPONSIBLE' OR partner_function = 'PERSON-RESPONSIBLE') AND transaction = transaction.id)) as 'employee_responsible'
FROM transaction) as txn
$$

DELIMITER ;