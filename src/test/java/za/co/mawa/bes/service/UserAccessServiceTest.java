package za.co.mawa.bes.service;

import org.junit.jupiter.api.Test;
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
}
