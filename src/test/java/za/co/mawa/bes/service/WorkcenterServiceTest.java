package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.dto.WorkcenterDto;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkcenterServiceTest {
    @Test
    void catalogueIsTheSourceForNavigationAndPermissionMetadata() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("cashup");
        when(rs.getString("description")).thenReturn("Cashups");
        when(rs.getString("route_path")).thenReturn("/cashups");
        when(rs.getString("group_code")).thenReturn("finance-payments");
        when(rs.getString("permission_code")).thenReturn("cashup");
        when(rs.getBoolean("active")).thenReturn(true);
        when(rs.getBoolean("assignable")).thenReturn(true);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> List.of(
                        ((org.springframework.jdbc.core.RowMapper<WorkcenterDto>) invocation.getArgument(1)).mapRow(rs, 0)));

        WorkcenterDto item = new WorkcenterService(jdbc).getAll().get(0);

        assertEquals("/cashups", item.getRoutePath());
        assertEquals("finance-payments", item.getGroupCode());
        assertEquals("cashup", item.getPermissionCode());
    }
}
