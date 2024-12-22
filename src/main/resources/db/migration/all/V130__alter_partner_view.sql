DELIMITER $$
DROP VIEW IF EXISTS `partner_view`$$
CREATE VIEW partner_view AS
SELECT
id AS 'partner_id',
number AS 'partner_no',
partner.type AS 'partner_type',
partner_role.role AS 'partner_role',
partner_identity.type AS 'identity_type',
partner_identity.value AS 'identity_number',
name1,
name2,
name3,
birth_date,
gender,
title AS 'title',
status AS 'status',
marital_status
FROM partner
LEFT JOIN partner_role ON partner.id = partner_role.partner
LEFT JOIN partner_identity ON partner.id = partner_identity.partner
$$
DELIMITER ;