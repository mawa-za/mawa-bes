package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackgroundExecutionContextServiceTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void establishesProtectedBgUserWithoutBusinessAuthorities() {
        UserService userService = mock(UserService.class);
        UserEntity bgUser = UserEntity.builder()
                .id("bg-user-id")
                .username("BGUSER")
                .partner(null)
                .accountType("STANDARD")
                .accessScope("BACKGROUND")
                .build();
        when(userService.getUserEntityByName("BGUSER")).thenReturn(bgUser);

        BackgroundExecutionContextService service = new BackgroundExecutionContextService(userService);
        service.establish();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("BGUSER");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
        assertThat(UserContext.getCurrentUser()).isEqualTo("BGUSER");
        assertThat(UserContext.getCurrentUserId()).isEqualTo("bg-user-id");
        assertThat(UserContext.isProtectedUser()).isTrue();
        assertThat(UserContext.isBackgroundSession()).isTrue();
        assertThat(UserContext.isExternalTransactionsBlocked()).isFalse();
    }

    @Test
    void clearsSecurityAndUserContexts() {
        UserService userService = mock(UserService.class);
        when(userService.getUserEntityByName("BGUSER")).thenReturn(null);
        BackgroundExecutionContextService service = new BackgroundExecutionContextService(userService);

        service.establish();
        service.clear();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(UserContext.getCurrentUser()).isNull();
        assertThat(UserContext.getCurrentUserId()).isNull();
        assertThat(UserContext.isBackgroundSession()).isFalse();
    }
}
