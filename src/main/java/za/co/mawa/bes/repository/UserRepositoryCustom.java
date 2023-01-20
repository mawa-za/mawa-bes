package za.co.mawa.bes.repository;

import za.co.mawa.bes.dto.RoleDto;

import java.util.List;

public interface UserRepositoryCustom
{
    List<RoleDto> findRoleByUser(String user);
}
