package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuneralClaimDeceasedGuardTest {
    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks FuneralManagementService service;

    @Test
    void pairedGroceryClaimOnSameFuneralServiceIsPermitted() {
        stubMainMember("DECEASED");
        when(jdbcTemplate.queryForList(startsWith("SELECT claim_type, funeral_service_id"), eq("M"), eq("P")))
                .thenReturn(List.of(Map.of("claim_type", "FUNERAL", "funeral_service_id", "FS-1")));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service,
                "requireFuneralClaimAllowed", null, "M", "P", "MAIN_MEMBER", "FS-1", "GROCERY", false));
    }

    @Test
    void unrelatedFuneralServiceCannotClaimAgain() {
        stubMainMember("DECEASED");
        when(jdbcTemplate.queryForList(startsWith("SELECT claim_type, funeral_service_id"), eq("M"), eq("P")))
                .thenReturn(List.of(Map.of("claim_type", "FUNERAL", "funeral_service_id", "FS-1")));

        assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "requireFuneralClaimAllowed", null, "M", "P", "MAIN_MEMBER", "FS-2", "FUNERAL", false));
    }

    @Test
    void cancelledClaimAllowsFreshFuneralRequestOnDeceasedMember() {
        stubMainMember("DECEASED");
        when(jdbcTemplate.queryForList(startsWith("SELECT claim_type, funeral_service_id"), eq("M"), eq("P")))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*) FROM"), eq(Integer.class),
                eq("M"), eq("P"))).thenReturn(1);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service,
                "requireFuneralClaimAllowed", null, "M", "P", "MAIN_MEMBER", "FS-2", "FUNERAL", false));
    }

    private void stubMainMember(String status) {
        when(jdbcTemplate.queryForList(startsWith("SELECT member_id, member_status"), eq("M")))
                .thenReturn(List.of(Map.of("member_id", "P", "member_status", status)));
    }
}
