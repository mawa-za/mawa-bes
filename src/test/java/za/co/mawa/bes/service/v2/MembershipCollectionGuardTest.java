package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MembershipCollectionGuardTest {
    @Test
    void collectionDoesNotUnlockLapsedCover() {
        var repository = mock(MembershipRepository.class);
        var membership = new MembershipEntity();
        membership.setId("m1");
        membership.setStatus("LAPSED");
        when(repository.findById("m1")).thenReturn(Optional.of(membership));
        var guard = new MembershipActionGuardService(repository, mock(JdbcTemplate.class));
        assertDoesNotThrow(() -> guard.requirePremiumCollectable("m1"));
        assertThrows(IllegalStateException.class, () -> guard.requireActionable(membership));
        assertEquals("LAPSED", membership.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void pendingReactivationAllowsCollectionButBlocksOtherActions() {
        var repository = mock(MembershipRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        var membership = new MembershipEntity();
        membership.setId("m1");
        membership.setStatus("INACTIVE");
        when(repository.findById("m1")).thenReturn(Optional.of(membership));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("m1"))).thenReturn(1);
        var guard = new MembershipActionGuardService(repository, jdbc);
        assertDoesNotThrow(() -> guard.requirePremiumCollectable("m1"));
        assertThrows(IllegalStateException.class, () -> guard.requireActionable(membership));
        assertEquals("INACTIVE", membership.getStatus());
    }

    @Test
    void mergedMembershipCannotCollect() {
        var repository = mock(MembershipRepository.class);
        var membership = new MembershipEntity();
        membership.setStatus("MERGED");
        when(repository.findById("m1")).thenReturn(Optional.of(membership));
        var guard = new MembershipActionGuardService(repository, mock(JdbcTemplate.class));
        assertThrows(IllegalStateException.class, () -> guard.requirePremiumCollectable("m1"));
    }
}
