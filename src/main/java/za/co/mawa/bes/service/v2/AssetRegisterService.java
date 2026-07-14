package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssetRegisterService {
    private final JdbcTemplate jdbcTemplate;

    public AssetRegisterService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public Map<String, Object> dashboard() {
        return jdbcTemplate.queryForMap(
                "SELECT COUNT(*) total_assets, " +
                        "COALESCE(SUM(CASE WHEN status='ACTIVE' THEN 1 ELSE 0 END),0) active_assets, " +
                        "COALESCE(SUM(CASE WHEN status='DISPOSED' THEN 1 ELSE 0 END),0) disposed_assets, " +
                        "COALESCE(SUM(CASE WHEN status<>'DISPOSED' THEN current_value ELSE 0 END),0) current_value " +
                        "FROM asset_register");
    }

    public List<Map<String, Object>> list(String query, String status, String category, String custodianPartnerId) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.*, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) custodian_name " +
                        "FROM asset_register a LEFT JOIN partner p ON p.id=a.custodian_partner_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (text(status)) { sql.append(" AND a.status=?"); args.add(status.trim().toUpperCase()); }
        if (text(category)) { sql.append(" AND a.category=?"); args.add(category.trim().toUpperCase()); }
        if (text(custodianPartnerId)) { sql.append(" AND a.custodian_partner_id=?"); args.add(custodianPartnerId); }
        if (text(query)) {
            sql.append(" AND (UPPER(a.asset_no) LIKE ? OR UPPER(a.name) LIKE ? OR UPPER(COALESCE(a.serial_no,'')) LIKE ? OR UPPER(COALESCE(a.barcode,'')) LIKE ?)");
            String like = "%" + query.trim().toUpperCase() + "%";
            args.add(like); args.add(like); args.add(like); args.add(like);
        }
        sql.append(" ORDER BY a.created_at DESC LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> get(String id) {
        Map<String, Object> asset = jdbcTemplate.queryForMap(
                "SELECT a.*, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) custodian_name " +
                        "FROM asset_register a LEFT JOIN partner p ON p.id=a.custodian_partner_id WHERE a.id=?", id);
        asset.put("history", jdbcTemplate.queryForList(
                "SELECT * FROM asset_register_event WHERE asset_id=? ORDER BY created_at DESC", id));
        return asset;
    }

    @Transactional
    public Map<String, Object> create(AssetRequest r, String userId) {
        validate(r);
        String id = UUID.randomUUID().toString();
        String no = text(r.assetNo()) ? r.assetNo().trim().toUpperCase() : nextNumber();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
                "INSERT INTO asset_register (id,asset_no,barcode,name,description,category,serial_no,acquisition_date,acquisition_cost,current_value,depreciation_method,useful_life_months,residual_value,location,custodian_partner_id,status,condition_status,warranty_expiry_date,notes,created_at,created_by,updated_at,updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,no,blank(r.barcode()),r.name().trim(),blank(r.description()),upper(r.category()),blank(r.serialNo()),date(r.acquisitionDate()),money(r.acquisitionCost()),money(r.currentValue()==null?r.acquisitionCost():r.currentValue()),upper(defaultValue(r.depreciationMethod(),"STRAIGHT_LINE")),r.usefulLifeMonths(),money(r.residualValue()),blank(r.location()),blank(r.custodianPartnerId()),upper(defaultValue(r.status(),"ACTIVE")),upper(defaultValue(r.conditionStatus(),"GOOD")),date(r.warrantyExpiryDate()),blank(r.notes()),now,userId,now,userId);
        event(id,"CREATE",null,no,r.notes(),userId);
        return get(id);
    }

    @Transactional
    public Map<String, Object> update(String id, AssetRequest r, String userId) {
        validate(r);
        Map<String, Object> existing = get(id);
        String assetNo = text(r.assetNo())
                ? r.assetNo().trim().toUpperCase()
                : String.valueOf(existing.get("asset_no"));
        jdbcTemplate.update(
                "UPDATE asset_register SET asset_no=?,barcode=?,name=?,description=?,category=?,serial_no=?,acquisition_date=?,acquisition_cost=?,current_value=?,depreciation_method=?,useful_life_months=?,residual_value=?,location=?,custodian_partner_id=?,status=?,condition_status=?,warranty_expiry_date=?,notes=?,updated_at=?,updated_by=? WHERE id=?",
                assetNo,blank(r.barcode()),r.name().trim(),blank(r.description()),upper(r.category()),blank(r.serialNo()),date(r.acquisitionDate()),money(r.acquisitionCost()),money(r.currentValue()),upper(defaultValue(r.depreciationMethod(),"STRAIGHT_LINE")),r.usefulLifeMonths(),money(r.residualValue()),blank(r.location()),blank(r.custodianPartnerId()),upper(defaultValue(r.status(),"ACTIVE")),upper(defaultValue(r.conditionStatus(),"GOOD")),date(r.warrantyExpiryDate()),blank(r.notes()),Timestamp.valueOf(LocalDateTime.now()),userId,id);
        event(id,"UPDATE",null,r.status(),r.notes(),userId);
        return get(id);
    }

    @Transactional
    public Map<String, Object> assign(String id, AssignmentRequest r, String userId) {
        Map<String,Object> old=get(id);
        jdbcTemplate.update("UPDATE asset_register SET custodian_partner_id=?,location=?,updated_at=?,updated_by=? WHERE id=?",blank(r.custodianPartnerId()),blank(r.location()),Timestamp.valueOf(LocalDateTime.now()),userId,id);
        event(id,"ASSIGN",String.valueOf(old.get("custodian_partner_id")),r.custodianPartnerId(),r.notes(),userId);
        return get(id);
    }

    @Transactional
    public Map<String, Object> dispose(String id, DisposalRequest r, String userId) {
        require(id);
        jdbcTemplate.update("UPDATE asset_register SET status='DISPOSED',disposal_date=?,disposal_proceeds=?,custodian_partner_id=NULL,updated_at=?,updated_by=? WHERE id=?",date(r.disposalDate()==null?LocalDate.now().toString():r.disposalDate()),money(r.disposalProceeds()),Timestamp.valueOf(LocalDateTime.now()),userId,id);
        event(id,"DISPOSE",null,"DISPOSED",r.notes(),userId);
        return get(id);
    }

    private void event(String assetId,String type,String oldValue,String newValue,String notes,String userId){
        jdbcTemplate.update("INSERT INTO asset_register_event (id,asset_id,event_type,old_value,new_value,notes,created_at,created_by) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),assetId,type,oldValue,newValue,notes,Timestamp.valueOf(LocalDateTime.now()),userId);
    }
    private String nextNumber(){
        Integer next=jdbcTemplate.queryForObject("SELECT COALESCE(MAX(CAST(SUBSTRING(asset_no,5) AS UNSIGNED)),0)+1 FROM asset_register WHERE asset_no LIKE 'AST-%'",Integer.class);
        return "AST-"+String.format("%06d",next==null?1:next);
    }
    private void require(String id){ Integer c=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM asset_register WHERE id=?",Integer.class,id); if(c==null||c==0) throw new IllegalArgumentException("Asset not found: "+id); }
    private void validate(AssetRequest r){ if(r==null||!text(r.name())) throw new IllegalArgumentException("Asset name is required"); if(text(r.assetNo())&&r.assetNo().length()>50) throw new IllegalArgumentException("Asset number is too long"); }
    private boolean text(String v){return v!=null&&!v.trim().isEmpty();}
    private String blank(String v){return text(v)?v.trim():null;}
    private String upper(String v){return text(v)?v.trim().toUpperCase():null;}
    private String defaultValue(String v,String d){return text(v)?v:d;}
    private BigDecimal money(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
    private Date date(String v){return text(v)?Date.valueOf(v):null;}

    public record AssetRequest(String assetNo,String barcode,String name,String description,String category,String serialNo,String acquisitionDate,BigDecimal acquisitionCost,BigDecimal currentValue,String depreciationMethod,Integer usefulLifeMonths,BigDecimal residualValue,String location,String custodianPartnerId,String status,String conditionStatus,String warrantyExpiryDate,String notes){}
    public record AssignmentRequest(String custodianPartnerId,String location,String notes){}
    public record DisposalRequest(String disposalDate,BigDecimal disposalProceeds,String notes){}
}
