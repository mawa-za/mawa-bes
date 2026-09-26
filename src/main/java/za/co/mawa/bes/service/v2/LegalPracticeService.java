package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LegalPracticeService {
    private final JdbcTemplate jdbc;
    private static final Set<String> RESOURCES = Set.of("intakes","conflicts","teams","access","proceedings","documents","correspondence","trust-reconciliations");
    private static final Map<String,String> TABLES = Map.of(
        "intakes","legal_intake", "conflicts","legal_conflict_check", "teams","legal_matter_team", "access","legal_matter_access",
        "proceedings","legal_proceeding", "documents","legal_document", "correspondence","legal_correspondence", "trust-reconciliations","legal_trust_reconciliation");
    private static final Map<String,Set<String>> ALLOWED = Map.of(
        "intakes", Set.of("prospect_partner_id","prospective_client_name","matter_type","summary","urgency","source","responsible_attorney_id","status","consultation_at","converted_case_id","decline_reason","created_by","updated_by"),
        "conflicts", Set.of("intake_id","case_id","search_terms","result","matches_json","decision_notes","waiver_required","waiver_document_id","checked_by","reviewed_at","reviewed_by"),
        "teams", Set.of("case_id","user_id","team_role","active","assigned_by"),
        "access", Set.of("case_id","principal_type","principal_id","access_scope","granted_by"),
        "proceedings", Set.of("case_id","proceeding_type","forum_name","division_name","case_number","presiding_officer","status","commenced_date","next_hearing_at","outcome","appeal_of_id","created_by"),
        "documents", Set.of("case_id","proceeding_id","attachment_id","document_type","title","version_no","status","confidentiality","filed_at","served_at","signed_at","service_method","service_proof_attachment_id","supersedes_document_id","created_by"),
        "correspondence", Set.of("case_id","direction","channel","subject","correspondent","body_summary","attachment_id","occurred_at","service_related","created_by"),
        "trust-reconciliations", Set.of("bank_account_id","period_end","statement_balance_cents","ledger_balance_cents","client_ledger_total_cents","difference_cents","status","notes","prepared_by","reviewed_by","reviewed_at","approved_by","approved_at")
    );

    public List<Map<String,Object>> list(String resource, String caseId) {
        validate(resource); String table=TABLES.get(resource);
        if (caseId != null && ALLOWED.get(resource).contains("case_id")) return jdbc.queryForList("SELECT * FROM "+table+" WHERE case_id=? ORDER BY 1 DESC", caseId);
        return jdbc.queryForList("SELECT * FROM "+table+" ORDER BY 1 DESC LIMIT 1000");
    }
    public Map<String,Object> get(String resource,String id) { validate(resource); return jdbc.queryForMap("SELECT * FROM "+TABLES.get(resource)+" WHERE id=?",id); }

    @Transactional public Map<String,Object> create(String resource, Map<String,Object> body) {
        validate(resource); String id=UUID.randomUUID().toString(); LinkedHashMap<String,Object> values=filtered(resource,body); values.put("id",id);
        insert(TABLES.get(resource), values); return get(resource,id);
    }
    @Transactional public Map<String,Object> update(String resource,String id,Map<String,Object> body) {
        validate(resource); LinkedHashMap<String,Object> values=filtered(resource,body); if(values.isEmpty()) return get(resource,id);
        String set=String.join(",", values.keySet().stream().map(k->k+"=?").toList()); List<Object> args=new ArrayList<>(values.values()); args.add(id);
        jdbc.update("UPDATE "+TABLES.get(resource)+" SET "+set+" WHERE id=?",args.toArray()); return get(resource,id);
    }
    @Transactional public void delete(String resource,String id) { validate(resource); jdbc.update("DELETE FROM "+TABLES.get(resource)+" WHERE id=?",id); }

    public Map<String,Object> dashboard() {
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("openMatters", scalar("SELECT COUNT(*) FROM legal_case WHERE status NOT IN ('CLOSED','CANCELLED')"));
        out.put("openIntakes", scalar("SELECT COUNT(*) FROM legal_intake WHERE status NOT IN ('CONVERTED','DECLINED')"));
        out.put("pendingConflicts", scalar("SELECT COUNT(*) FROM legal_conflict_check WHERE result='PENDING_REVIEW'"));
        out.put("upcomingHearings", scalar("SELECT COUNT(*) FROM legal_proceeding WHERE next_hearing_at BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY)"));
        out.put("draftDocuments", scalar("SELECT COUNT(*) FROM legal_document WHERE status IN ('DRAFT','REVIEW')"));
        out.put("unreconciledTrust", scalar("SELECT COUNT(*) FROM legal_trust_reconciliation WHERE status NOT IN ('APPROVED','LOCKED')"));
        return out;
    }
    private Long scalar(String sql){ Number n=jdbc.queryForObject(sql,Number.class); return n==null?0:n.longValue(); }
    private void validate(String resource){ if(!RESOURCES.contains(resource)) throw new IllegalArgumentException("Unsupported legal resource: "+resource); }
    private LinkedHashMap<String,Object> filtered(String resource,Map<String,Object> body){ LinkedHashMap<String,Object> v=new LinkedHashMap<>(); body.forEach((k,val)->{ if(ALLOWED.get(resource).contains(k)) v.put(k,val); }); return v; }
    private void insert(String table,LinkedHashMap<String,Object> v){ String cols=String.join(",",v.keySet()); String qs=String.join(",",Collections.nCopies(v.size(),"?")); jdbc.update("INSERT INTO "+table+" ("+cols+") VALUES ("+qs+")",v.values().toArray()); }
}
