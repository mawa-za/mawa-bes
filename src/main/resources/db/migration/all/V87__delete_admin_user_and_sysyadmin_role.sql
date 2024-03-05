DELETE FROM `user_role` WHERE (`role` = 'SYSADMIN');

DELETE FROM `user` WHERE (`username` = 'admin');

DELETE FROM `role_workcenter` WHERE (`role` = 'SYSADMIN');

DELETE FROM `role` WHERE (`id` = 'SYSADMIN');
