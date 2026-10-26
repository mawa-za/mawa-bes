package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.MembershipChangeRequestEntity;
import za.co.mawa.bes.enums.MembershipChangeType;
import za.co.mawa.bes.repository.v2.MembershipChangeRequestRepository;

import java.time.LocalDate;

/**
 * Resolves date-change comparisons from the original, persisted change request.
 * The membership's live dates must never replace the original approval snapshot.
 * This is response-only: historical approval rows and audit evidence are not edited.
 */
@Component
@RequiredArgsConstructor
public class MembershipDateApprovalPayloadResolver {
    private final MembershipChangeRequestRepository changeRequestRepository;
    private final ObjectMapper objectMapper;

    public String resolve(String changeRequestId, String existingPayloadJson) {
        if (changeRequestId == null || changeRequestId.isBlank()) return existingPayloadJson;
        return changeRequestRepository.findById(changeRequestId)
                .filter(change -> change.getChangeType() == MembershipChangeType.DATE_CHANGE)
                .map(change -> addStoredDates(change, existingPayloadJson))
                .orElse(existingPayloadJson);
    }

    private String addStoredDates(MembershipChangeRequestEntity change, String payloadJson) {
        try {
            JsonNode decoded = payloadJson == null || payloadJson.isBlank()
                    ? objectMapper.createObjectNode() : objectMapper.readTree(payloadJson);
            if (decoded == null || !decoded.isObject()) return payloadJson;
            ObjectNode root = (ObjectNode) decoded;
            ObjectNode current = objectNode(root, "currentValues");
            ObjectNode proposed = objectNode(root, "proposedValues");

            putDate(current, "startDate", change.getOldStartDate());
            putDate(current, "effectiveDate", change.getOldMembershipEffectiveDate());
            putDate(proposed, "startDate", change.getNewStartDate());
            putDate(proposed, "effectiveDate", change.getNewMembershipEffectiveDate());

            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            // Never replace a malformed historical payload with invented approval details.
            return payloadJson;
        }
    }

    private static ObjectNode objectNode(ObjectNode root, String key) {
        JsonNode existing = root.get(key);
        if (existing instanceof ObjectNode object) return object;
        ObjectNode created = root.objectNode();
        root.set(key, created);
        return created;
    }

    private static void putDate(ObjectNode target, String key, LocalDate date) {
        if (date == null) target.putNull(key);
        else target.put(key, date.toString());
    }
}
