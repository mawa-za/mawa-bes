package za.co.mawa.bes.controller.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.configuration.context.UserContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/pay-app/manual-actions")
public class PayAppManualActionControllerV2 {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody Map<String, Object> request) throws Exception {
        String payload = objectMapper.writeValueAsString(request.get("payload"));
        if (payload.equals("null") || payload.equals("{}")) throw new IllegalArgumentException("Failed payload is required");
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO pay_app_manual_action
                    (id, device_id, entity_type, endpoint, http_method, payload_json,
                     failure_response, status, requested_by, requested_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, UTC_TIMESTAMP())
                """, id, text(request.get("deviceId")), text(request.get("entityType")),
                text(request.get("endpoint")), text(request.get("method")), payload,
                text(request.get("failureResponse")), actor());
        return ResponseEntity.accepted().body(Map.of("id", id, "status", "PENDING"));
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "PENDING") String status) {
        return jdbcTemplate.queryForList("SELECT * FROM pay_app_manual_action WHERE status=? ORDER BY requested_at DESC", status);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, Object> request) {
        String status = text(request.get("status")).toUpperCase();
        if (!List.of("PENDING", "IN_PROGRESS", "COMPLETED", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("Invalid manual action status");
        }
        jdbcTemplate.update("UPDATE pay_app_manual_action SET status=?, action_notes=?, actioned_by=?, actioned_at=UTC_TIMESTAMP() WHERE id=?",
                status, text(request.get("notes")), actor(), id);
        return ResponseEntity.ok(Map.of("id", id, "status", status));
    }

    private String actor() {
        String id = UserContext.getCurrentUserId();
        return id == null || id.isBlank() ? "DEVICE" : id;
    }

    private String text(Object value) { return value == null ? "" : value.toString().trim(); }
}
