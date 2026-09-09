package za.co.mawa.bes.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.WorkcenterDao;
import za.co.mawa.bes.dto.WorkcenterDto;
import za.co.mawa.bes.exception.RoleDoesNotExist;

import java.util.List;

@Service
public class WorkcenterService implements WorkcenterDao {
    private final JdbcTemplate jdbcTemplate;

    public WorkcenterService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<WorkcenterDto> getAll() {
        return queryCatalogue("WHERE active = TRUE AND assignable = TRUE", new Object[0]);
    }

    @Override
    public WorkcenterDto getById(String id) throws RoleDoesNotExist {
        return queryCatalogue("WHERE LOWER(id) = LOWER(?) AND active = TRUE", new Object[]{id})
                .stream().findFirst().orElseThrow(RoleDoesNotExist::new);
    }

    private List<WorkcenterDto> queryCatalogue(String where, Object[] arguments) {
        String sql = """
                SELECT id, description, default_function, route_key, route_path, icon_key,
                       group_code, group_title, group_description, section_code, section_title,
                       section_display_order, group_display_order, display_order,
                       card_description, active, assignable, permission_code
                  FROM workcenter_catalog
                """ + where + " ORDER BY section_display_order, group_display_order, display_order, description";
        return jdbcTemplate.query(sql, (rs, rowNum) -> WorkcenterDto.catalogued(
                rs.getString("id"), rs.getString("description"), rs.getString("default_function"),
                rs.getString("route_key"), rs.getString("route_path"), rs.getString("icon_key"),
                rs.getString("group_code"), rs.getString("group_title"), rs.getString("group_description"),
                rs.getString("section_code"), rs.getString("section_title"),
                rs.getInt("section_display_order"), rs.getInt("group_display_order"),
                rs.getInt("display_order"), rs.getString("card_description"),
                rs.getBoolean("active"), rs.getBoolean("assignable"),
                rs.getString("permission_code")), arguments);
    }
}

