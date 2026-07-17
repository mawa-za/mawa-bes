package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class PaymentAccountConfigurationService {
    private final JdbcTemplate jdbc;
    public List<Map<String,Object>> list(){ return jdbc.queryForList("SELECT * FROM payment_bank_account ORDER BY account_role, request_type, bank_name"); }
    @Transactional public Map<String,Object> save(Map<String,Object> request){
        String id = Objects.toString(request.get("id"), "");
        if(id.isBlank()) id=UUID.randomUUID().toString();
        String role=req(request,"accountRole");
        String type=blank(request.get("requestType"));
        if("DEBTOR".equals(role) && (type==null || type.isBlank())) throw new IllegalArgumentException("requestType is required for debtor accounts");
        jdbc.update("UPDATE payment_bank_account SET active=0 WHERE account_role=? AND ((request_type IS NULL AND ? IS NULL) OR request_type=?) AND id<>?",role,type,type,id);
        jdbc.update("""
            INSERT INTO payment_bank_account(id,account_role,request_type,bank_integration,bank_name,account_holder,account_number,branch_code,account_type,partner_id,active)
            VALUES(?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE account_role=VALUES(account_role),request_type=VALUES(request_type),bank_integration=VALUES(bank_integration),bank_name=VALUES(bank_name),account_holder=VALUES(account_holder),account_number=VALUES(account_number),branch_code=VALUES(branch_code),account_type=VALUES(account_type),partner_id=VALUES(partner_id),active=VALUES(active)
            """, id,role,type,blank(request.get("bankIntegration")),req(request,"bankName"),req(request,"accountHolder"),req(request,"accountNumber"),blank(request.get("branchCode")),blank(request.get("accountType")),blank(request.get("partnerId")),bool(request.get("active"),true));
        return jdbc.queryForMap("SELECT * FROM payment_bank_account WHERE id=?",id);
    }
    @Transactional public void deactivate(String id){ jdbc.update("UPDATE payment_bank_account SET active=0 WHERE id=?",id); }
    public Optional<Map<String,Object>> activeDebtor(String type){ var l=jdbc.queryForList("SELECT * FROM payment_bank_account WHERE account_role='DEBTOR' AND request_type=? AND active=1 ORDER BY updated_at DESC LIMIT 1",type); return l.stream().findFirst(); }
    public Optional<Map<String,Object>> activeCreditor(String role){ var l=jdbc.queryForList("SELECT * FROM payment_bank_account WHERE account_role=? AND active=1 ORDER BY updated_at DESC LIMIT 1",role); return l.stream().findFirst(); }
    private static String req(Map<String,Object> m,String k){String v=blank(m.get(k));if(v==null||v.isBlank())throw new IllegalArgumentException(k+" is required");return v;}
    private static String blank(Object v){return v==null?null:v.toString().trim();}
    private static boolean bool(Object v,boolean d){return v==null?d:Boolean.parseBoolean(v.toString());}
}
