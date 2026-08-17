package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.service.UserService;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrossTenantExecutionServiceTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void crossTenantWorkerUsesBgUserAndDoesNotPropagateHumanIdentity() {
        UserService userService = mock(UserService.class);
        UserEntity bgUser = UserEntity.builder()
                .id("bg-id")
                .username("BGUSER")
                .accountType("STANDARD")
                .accessScope("BACKGROUND")
                .build();
        when(userService.getUserEntityByName("BGUSER")).thenReturn(bgUser);

        BackgroundExecutionContextService background = new BackgroundExecutionContextService(userService);
        Executor directExecutor = Runnable::run;
        CrossTenantExecutionService service = new CrossTenantExecutionService(directExecutor, background);

        String result = service.execute("tenant_target", "human.user", "human-id", () -> {
            assertThat(TenantContext.getCurrentTenant()).isEqualTo("tenant_target");
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("BGUSER");
            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
            assertThat(UserContext.getCurrentUser()).isEqualTo("BGUSER");
            assertThat(UserContext.getCurrentUserId()).isEqualTo("bg-id");
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(TenantContext.getCurrentTenant()).isNull();
        assertThat(UserContext.getCurrentUser()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
