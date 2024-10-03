DELIMITER $$
DROP VIEW IF EXISTS `partner_view`$$
CREATE VIEW partner_view AS
select
id AS 'partner_id',
number AS 'partner_no',
partner.type as 'partner_type',
partner_role.role as 'partner_role',
partner_identity.type as 'identity_type',
partner_identity.value as 'identity_number',
name1,
name2,
name3,
birth_date,
gender,
title AS 'title',
status AS 'status',
marital_status
from partner
inner join partner_role on partner.id = partner_role.partner
inner join partner_identity on partner.id = partner_identity.partner
$$
DELIMITER ;