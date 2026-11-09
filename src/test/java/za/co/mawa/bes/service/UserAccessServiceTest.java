package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.access.UserAccessProfileDto;
import za.co.mawa.bes.entity.RoleEntity;
import za.co.mawa.bes.entity.RoleWorkcenterPKEntity;
import za.co.mawa.bes.repository.RoleRepository;
import za.co.mawa.bes.repository.RoleWorkcenterRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccessServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private RoleWorkcenterRepository roleWorkcenterRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @Test
    void selectedRoleControlsConfigurationAccessEvenWhenAnotherRoleHasFullAccess() {
        UserContext.clear();
        UserAccessService service = spy(new UserAccessService());
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(service, "roleWorkcenterRepository", roleWorkcenterRepository);

        UserAccessProfileDto profile = UserAccessProfileDto.builder()
                .roles(List.of("LIMITED", "ADMIN"))
                .allWorkcentres(true)
                .build();
        doReturn(profile).when(service).profile();

        RoleEntity limited = new RoleEntity();
        limited.setId("LIMITED");
        limited.setAccessAllWorkcentres(false);
        RoleEntity admin = new RoleEntity();
        admin.setId("ADMIN");
        admin.setAccessAllWorkcentres(true);

        when(roleRepository.findById("LIMITED")).thenReturn(Optional.of(limited));
        when(roleRepository.findById("ADMIN")).thenReturn(Optional.of(admin));
        when(roleWorkcenterRepository.existsById(any(RoleWorkcenterPKEntity.class)))
                .thenReturn(false);

        assertFalse(service.hasWorkcentreAccess("approval-workflow", "LIMITED"));
        assertTrue(service.hasWorkcentreAccess("approval-workflow", "ADMIN"));
    }

    @Test
    void workcentreAllAccessDoesNotGrantBusinessActionPermission() {
        UserContext.clear();
        UserAccessService service = spy(new UserAccessService());
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbcTemplate);
        doReturn(UserAccessProfileDto.builder().roles(List.of("ADMIN"))
                .allWorkcentres(true).build()).when(service).profile();

        when(jdbcTemplate.queryForObject(any(String.class),
                org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq("ADMIN"),
                org.mockito.ArgumentMatchers.eq("premium-payment-cancel"))).thenReturn(0);

        assertFalse(service.hasPermission("premium-payment-cancel", "ADMIN"));
    }

    @Test
    void actionPermissionHonoursSelectedRoleRatherThanOtherAssignedRole() {
        UserContext.clear();
        UserAccessService service = spy(new UserAccessService());
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbcTemplate);
        doReturn(UserAccessProfileDto.builder().roles(List.of("CASHIER", "ADMIN"))
                .allWorkcentres(true).build()).when(service).profile();

        when(jdbcTemplate.queryForObject(any(String.class),
                org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq("CASHIER"),
                org.mockito.ArgumentMatchers.eq("premium-payment-cancel"))).thenReturn(0);
        when(jdbcTemplate.queryForObject(any(String.class),
                org.mockito.ArgumentMatchers.eq(Integer.class),
                org.mockito.ArgumentMatchers.eq("ADMIN"),
                org.mockito.ArgumentMatchers.eq("premium-payment-cancel"))).thenReturn(1);

        assertFalse(service.hasPermission("premium-payment-cancel", "CASHIER"));
        assertTrue(service.hasPermission("premium-payment-cancel", "ADMIN"));
        assertFalse(service.hasPermission("premium-payment-create", "IMPERSONATED_ROLE"));
    }

}
