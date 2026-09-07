package za.co.mawa.bes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import za.co.mawa.bes.configuration.context.UserContext;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PayAppManualActionService {
    private static final Map<String, String> ALLOWED = Map.of(
            "/v2/partner", "PARTNER", "/v2/membership", "MEMBERSHIP",
            "/v2/sync/payment-batches/membership-premiums", "PAYMENT_BATCH",
            "/v2/sync/payment-batches/group-society", "PAYMENT_BATCH", "/v2/cashup", "CASHUP");
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final RestTemplate rest = new RestTemplate();
    @Value("${device-sync.internal-base-url:http://127.0.0.1:${server.port:8080}}") private String internalBaseUrl;

    public Map<String, Object> submit(Map<String, Object> request, HttpHeaders headers) throws Exception {
        Map<String, Object> body = payload(request.get("payload"));
        String path = safePath(request.get("endpoint"));
        String type = required(request.get("entityType"), "entityType").toUpperCase(Locale.ROOT);
        validateTarget(path, required(request.get("method"), "method"), type);
        String device = required(request.get("deviceId"), "deviceId");
        String local = required(request.get("localRecordId"), "localRecordId");
        verifyIdentity(type, device, local, body);
        String key = key(type, device, local, body);
        String json = mapper.writeValueAsString(body);
        List<Map<String,Object>> existing = jdbc.queryForList("SELECT * FROM pay_app_manual_action WHERE idempotency_key=? ORDER BY requested_at DESC LIMIT 1", key);
        if (!existing.isEmpty()) {
            Map<String,Object> current = new LinkedHashMap<>(existing.get(0));
            String status = text(current.get("status"));
            if ("COMPLETED".equals(status) || "PROCESSING".equals(status) || "CORRECTION_REQUIRED".equals(status)) return current;
            if (Boolean.TRUE.equals(request.get("automatic")) && ((Number)current.get("attempt_count")).intValue() < 3) {
                return processInternal(text(current.get("id")), headers, false);
            }
            return current;
        }
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO pay_app_manual_action (id,device_id,entity_type,local_record_id,idempotency_key,endpoint,http_method,payload_json,original_payload_json,failure_response,status,requested_by,requested_at) VALUES (?,?,?,?,?,?,'POST',?,?,?,'PENDING',?,UTC_TIMESTAMP())",
                id, device, type, local, key, path, json, json, text(request.get("failureResponse")), actor());
        return Boolean.TRUE.equals(request.get("automatic"))
                ? processInternal(id, headers, false)
                : internal(id);
    }

    public List<Map<String, Object>> list(String status, String search) {
        requireAdmin();
        StringBuilder sql = new StringBuilder("SELECT * FROM pay_app_manual_action WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!blank(status) && !"ALL".equalsIgnoreCase(status)) {
            if ("ATTENTION_REQUIRED".equalsIgnoreCase(status)) sql.append(" AND status IN ('PENDING','FAILED','CORRECTION_REQUIRED')");
            else { sql.append(" AND status=?"); args.add(status.toUpperCase(Locale.ROOT)); }
        }
        if (!blank(search)) {
            sql.append(" AND (LOWER(id) LIKE ? OR LOWER(device_id) LIKE ? OR LOWER(entity_type) LIKE ? OR LOWER(endpoint) LIKE ? OR LOWER(COALESCE(last_error,'')) LIKE ?)");
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            for (int i=0; i<5; i++) args.add(term);
        }
        sql.append(" ORDER BY requested_at DESC LIMIT 200");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> get(String id) {
        requireAdmin();
        Map<String, Object> row = internal(id);
        row.put("attempts", jdbc.queryForList("SELECT * FROM pay_app_manual_action_attempt WHERE manual_action_id=? ORDER BY attempt_no DESC", id));
        return row;
    }

    public Map<String, Object> reconcile(String deviceId, String entityType, String localRecordId) {
        String idem = key(entityType.toUpperCase(Locale.ROOT), deviceId, localRecordId);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT response_body,response_status FROM pay_app_manual_action WHERE idempotency_key=? AND status='COMPLETED' ORDER BY completed_at DESC LIMIT 1", idem);
        if (rows.isEmpty()) return Map.of("verified", false, "message", "No completed manual action was found");
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("verified", true); response.put("entityType", entityType.toUpperCase(Locale.ROOT));
        response.put("message", "Completed manual action verified"); response.put("responseStatus", rows.get(0).get("response_status"));
        try {
            Object body = mapper.readValue(text(rows.get(0).get("response_body")), Object.class);
            response.put("response", body);
            if (body instanceof Map<?,?> map) for (String name : List.of("id","serverId","partnerId","membershipId","cashupId","paymentBatchId")) if(map.get(name)!=null){response.put("serverRecordId",map.get(name).toString());break;}
        } catch (Exception ignored) { response.put("response", rows.get(0).get("response_body")); }
        return response;
    }

    public Map<String, Object> correct(String id, Map<String, Object> request) throws Exception {
        requireAdmin();
        Map<String, Object> current = internal(id);
        if (Set.of("PROCESSING", "COMPLETED").contains(text(current.get("status")))) throw new IllegalStateException("A processing or completed action cannot be corrected");
        Map<String, Object> body = payload(request.get("payload"));
        verifyIdentity(text(current.get("entity_type")), text(current.get("device_id")), text(current.get("local_record_id")), body);
        jdbc.update("UPDATE pay_app_manual_action SET payload_json=?,status='PENDING',action_notes=?,actioned_by=?,actioned_at=UTC_TIMESTAMP(),response_status=NULL,response_body=NULL,last_error=NULL,row_version=row_version+1 WHERE id=?",
                mapper.writeValueAsString(body), text(request.get("notes")), actor(), id);
        return internal(id);
    }

    public Map<String, Object> process(String id, HttpHeaders incoming) throws Exception {
        return processInternal(id, incoming, true);
    }

    private Map<String, Object> processInternal(String id, HttpHeaders incoming, boolean administratorRequired) throws Exception {
        if (administratorRequired) requireAdmin();
        Map<String, Object> beforeClaim = internal(id);
        String path = safePath(beforeClaim.get("endpoint"));
        String type = text(beforeClaim.get("entity_type"));
        validateTarget(path, text(beforeClaim.get("http_method")), type);
        Map<String, Object> beforePayload = mapper.readValue(
                text(beforeClaim.get("payload_json")), new TypeReference<>() {});
        String idem = key(type, text(beforeClaim.get("device_id")),
                text(beforeClaim.get("local_record_id")), beforePayload);
        int claimed = jdbc.update("UPDATE pay_app_manual_action SET status='PROCESSING',processing_started_at=UTC_TIMESTAMP(),attempt_count=attempt_count+1,row_version=row_version+1,actioned_by=?,actioned_at=UTC_TIMESTAMP() WHERE id=? AND status IN ('PENDING','FAILED','CORRECTION_REQUIRED')", actor(), id);
        if (claimed != 1) throw new IllegalStateException("Action is already processing or is no longer executable");
        Map<String, Object> action = internal(id);
        int attempt = ((Number)action.get("attempt_count")).intValue();
        String attemptId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO pay_app_manual_action_attempt (id,manual_action_id,attempt_no,endpoint,http_method,payload_json,idempotency_key,status,attempted_by,started_at) VALUES (?,?,?,?,'POST',?,?,'PROCESSING',?,UTC_TIMESTAMP())",
                attemptId, id, attempt, path, action.get("payload_json"), idem, actor());
        try {
            HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
            for (String h : List.of(HttpHeaders.AUTHORIZATION,"X-TenantID","X-Tenant-Id","X-UserID","X-User-Id")) if (incoming.getFirst(h)!=null) headers.set(h,incoming.getFirst(h));
            headers.set("X-Idempotency-Key", idem);
            Object body = mapper.readValue(text(action.get("payload_json")), Object.class);
            ResponseEntity<String> response = rest.exchange(internalBaseUrl+path,HttpMethod.POST,new HttpEntity<>(body,headers),String.class);
            finish(id,attemptId,"COMPLETED",response.getStatusCode().value(),response.getBody(),null);
        } catch (HttpStatusCodeException ex) {
            finish(id,attemptId,ex.getStatusCode().is4xxClientError()?"CORRECTION_REQUIRED":"FAILED",ex.getStatusCode().value(),ex.getResponseBodyAsString(),ex.getMessage());
        } catch (Exception ex) { finish(id,attemptId,"FAILED",null,null,ex.getMessage()); }
        // Automatic device recovery is intentionally not granted ERP workcentre
        // access. Return the processed row directly; administrator detail/history
        // remains protected by get(id).
        return internal(id);
    }

    public Map<String, Object> reject(String id, String notes) {
        requireAdmin();
        if (jdbc.update("UPDATE pay_app_manual_action SET status='REJECTED',action_notes=?,actioned_by=?,actioned_at=UTC_TIMESTAMP(),row_version=row_version+1 WHERE id=? AND status<>'PROCESSING'",notes,actor(),id)!=1)
            throw new IllegalStateException("A processing action cannot be rejected");
        return internal(id);
    }

    private void finish(String id,String attempt,String status,Integer code,String response,String error) {
        jdbc.update("UPDATE pay_app_manual_action SET status=?,response_status=?,response_body=?,last_error=?,completed_at=CASE WHEN ?='COMPLETED' THEN UTC_TIMESTAMP() ELSE NULL END,processing_started_at=NULL,row_version=row_version+1 WHERE id=?",status,code,response,error,status,id);
        jdbc.update("UPDATE pay_app_manual_action_attempt SET status=?,response_status=?,response_body=?,error_message=?,completed_at=UTC_TIMESTAMP() WHERE id=?",status,code,response,error,attempt);
    }
    private Map<String,Object> internal(String id) { List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM pay_app_manual_action WHERE id=?",id); if(rows.isEmpty())throw new NoSuchElementException("Manual sync action not found"); return new LinkedHashMap<>(rows.get(0)); }
    private void validateTarget(String path,String method,String type){ if(!"POST".equalsIgnoreCase(method))throw new IllegalArgumentException("Only POST manual actions are supported"); if(!type.equals(ALLOWED.get(path)))throw new IllegalArgumentException("Endpoint is not allowed for this entity type"); }
    private void verifyIdentity(String type,String device,String local,Map<String,Object> body){ if("PAYMENT_BATCH".equals(type)&&(!device.equals(text(body.get("deviceId")))||!local.equals(text(body.get("localPaymentBatchId")))))throw new IllegalArgumentException("Payment batch identity cannot be changed"); if("CASHUP".equals(type)&&!device.equals(text(body.get("deviceId"))))throw new IllegalArgumentException("Cashup device identity cannot be changed"); }
    private String key(String type,String device,String local){ return key(type,device,local,null); }
    private String key(String type,String device,String local,Map<String,Object> body){
        String normalizedDevice=required(device,"deviceId");
        if("PAYMENT_BATCH".equals(type)&&body!=null&&!blank(text(body.get("paymentBatchNo")))) return "payment-batch-number:"+normalizedDevice+":"+text(body.get("paymentBatchNo"));
        String prefix=switch(type){case"PAYMENT_BATCH"->"payment-batch";case"PARTNER"->"partner";case"MEMBERSHIP"->"membership";case"CASHUP"->"cashup";default->throw new IllegalArgumentException("Unsupported entity type");}; return prefix+":"+normalizedDevice+":"+required(local,"localRecordId"); }
    private Map<String,Object> payload(Object value){ if(value==null)throw new IllegalArgumentException("Failed payload is required"); Map<String,Object> result=mapper.convertValue(value,new TypeReference<>(){}); if(result.isEmpty())throw new IllegalArgumentException("Failed payload is required"); return result; }
    private String safePath(Object value){ String path=required(value,"endpoint"); if(path.contains("?")||path.contains("..")||path.contains("://"))throw new IllegalArgumentException("Invalid manual action endpoint"); return path; }
    private void requireAdmin(){
        String user = !blank(UserContext.getCurrentUserId()) ? UserContext.getCurrentUserId() : UserContext.getCurrentUser();
        if (blank(user)) throw new AccessDeniedException("An authenticated ERP administrator is required");
        Integer allowed = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_role ur
                JOIN role r ON r.id=ur.role
                LEFT JOIN role_workcenter rw ON rw.role=r.id AND rw.workcenter='device-sync-correction'
                WHERE ur.user=?
                  AND (ur.valid_from IS NULL OR ur.valid_from<=CURRENT_DATE)
                  AND (ur.valid_to IS NULL OR ur.valid_to>=CURRENT_DATE)
                  AND (r.valid_from IS NULL OR r.valid_from<=CURRENT_DATE)
                  AND (r.valid_to IS NULL OR r.valid_to>=CURRENT_DATE)
                  AND (COALESCE(r.access_all_workcentres,0)=1 OR rw.workcenter IS NOT NULL)
                """, Integer.class, user);
        if (allowed == null || allowed == 0) throw new AccessDeniedException("Device Sync Corrections access is required");
    }
    private String actor(){ return !blank(UserContext.getCurrentUserId())?UserContext.getCurrentUserId():!blank(UserContext.getCurrentUser())?UserContext.getCurrentUser():"DEVICE"; }
    private String required(Object value,String name){String v=text(value);if(v.isBlank())throw new IllegalArgumentException(name+" is required");return v;}
    private boolean blank(String value){return value==null||value.isBlank();} private String text(Object value){return value==null?"":value.toString().trim();}
}
