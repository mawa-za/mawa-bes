package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.RoleDao;
import za.co.mawa.bes.dto.RoleDto;
@Service
@Transactional
public class RoleService implements RoleDao {
    @Override
    public void create(RoleDto roleDto) {

    }
}
