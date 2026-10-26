package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.v2.MembershipChangeRequestEntity;
import za.co.mawa.bes.enums.MembershipChangeType;
import za.co.mawa.bes.repository.v2.MembershipChangeRequestRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipDateApprovalPayloadResolverTest {
    @Mock private MembershipChangeRequestRepository changes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void restoresOriginalDateSnapshotWithoutChangingOtherApprovalDetails() throws Exception {
        MembershipChangeRequestEntity change = MembershipChangeRequestEntity.builder()
                .id("date-request-1")
                .changeType(MembershipChangeType.DATE_CHANGE)
                .oldStartDate(LocalDate.parse("2025-01-01"))
                .newStartDate(LocalDate.parse("2025-03-01"))
                .oldMembershipEffectiveDate(LocalDate.parse("2025-04-01"))
                .newMembershipEffectiveDate(LocalDate.parse("2025-06-01"))
                .build();
        when(changes.findById("date-request-1")).thenReturn(Optional.of(change));

        String original = """
                {"requestSummary":{"reason":"Historical correction","membershipNumber":"1000043019"},
                 "currentValues":{"startDate":"N/A","effectiveDate":"N/A"},
                 "proposedValues":{"startDate":"N/A","effectiveDate":"N/A"},
                 "approvalImpact":{"premiumAdjustmentPreview":{"premiumRecordsBeforeNewStart":0}}}
                """;

        JsonNode result = objectMapper.readTree(new MembershipDateApprovalPayloadResolver(changes, objectMapper)
                .resolve("date-request-1", original));
        assertEquals("2025-01-01", result.at("/currentValues/startDate").asText());
        assertEquals("2025-03-01", result.at("/proposedValues/startDate").asText());
        assertEquals("2025-04-01", result.at("/currentValues/effectiveDate").asText());
        assertEquals("2025-06-01", result.at("/proposedValues/effectiveDate").asText());
        assertEquals("Historical correction", result.at("/requestSummary/reason").asText());
        assertEquals(0, result.at("/approvalImpact/premiumAdjustmentPreview/premiumRecordsBeforeNewStart").asInt());
    }

    @Test
    void missingHistoricalEffectiveDateIsNullRatherThanInvented() throws Exception {
        MembershipChangeRequestEntity change = MembershipChangeRequestEntity.builder()
                .changeType(MembershipChangeType.DATE_CHANGE)
                .newStartDate(LocalDate.parse("2026-10-01"))
                .build();
        when(changes.findById("change-2")).thenReturn(Optional.of(change));
        JsonNode result = objectMapper.readTree(new MembershipDateApprovalPayloadResolver(changes, objectMapper)
                .resolve("change-2", "{}"));
        assertEquals("2026-10-01", result.at("/proposedValues/startDate").asText());
        assertEquals(true, result.at("/currentValues/startDate").isNull());
        assertEquals(true, result.at("/proposedValues/effectiveDate").isNull());
    }

    @Test
    void leavesPayloadUntouchedWhenSourceRequestCannotBeLocated() {
        when(changes.findById("old-reference")).thenReturn(Optional.empty());
        String original = "{\"currentValues\":{\"startDate\":\"N/A\"}}";
        assertEquals(original, new MembershipDateApprovalPayloadResolver(changes, objectMapper)
                .resolve("old-reference", original));
    }
}
