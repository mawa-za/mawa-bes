DELIMITER $$
DROP VIEW IF EXISTS `partner_view`$$
CREATE VIEW partner_view AS
Select 
partner_id,
partner_no,
partner_type,
partner_type_desc,
role as partner_role,
partner_identity_number,
name1,
name2,
name3,
title,
title_desc,
status,
status_desc,
concat(partner_identity_number,name1,name2,name3) as filter
from 
(select
id AS 'partner_id',
number AS 'partner_no',
type as 'partner_type',
getFieldOptionDesc('PARTNER-TYPE', type) AS 'partner_type_desc',
getPartnerIdentityNumber('SA-ID',id) as 'partner_identity_number',
name1,
name2,
name3,
title AS 'title',
getFieldOptionDesc('TITLE', title) AS 'title_desc',
status AS 'status',
getFieldOptionDesc('PARTNER-STATUS', status) AS 'status_desc'
from partner ) as partner
inner join partner_role on partner.partner_id = partner_role.partner
$$
DELIMITER ;