package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.service.NumberRangeService;
import za.co.mawa.bes.service.SettingService;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FuneralResourcePlanningService {
    public static final String GROUP = "FUNERAL-RESOURCE-PLANNING";
    private static final Set<String> LIVE = Set.of("PROVISIONAL", "CONFIRMED", "ISSUED", "ORDERED");
    private final JdbcTemplate jdbc;
    private final SettingService settings;
    private final NumberRangeService numbers;

    public FuneralResourcePlanningService(JdbcTemplate jdbc, SettingService settings, NumberRangeService numbers) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.numbers = numbers;
    }

    public Map<String,Object> configuration() {
        String status = value("STATUS", "DISABLED").toUpperCase(Locale.ROOT);
        return new LinkedHashMap<>(Map.of(
                "status", status,
                "enabled", "ENABLED".equals(status),
                "autoCreatePlans", bool("AUTO-CREATE-PLANS", true),
                "requireReadiness", bool("REQUIRE-READINESS", true),
                "provisionalExpiryHours", integer("PROVISIONAL-EXPIRY-HOURS", 2),
                "allowThirdPartyLeasing", bool("ALLOW-THIRD-PARTY-LEASING", true),
                "requireApprovedPo", bool("REQUIRE-APPROVED-PO", true),
                "defaultAssignedEmployeeId", value("DEFAULT-ASSIGNED-EMPLOYEE-ID", ""),
                "defaultAssignedRoleId", value("DEFAULT-ASSIGNED-ROLE-ID", "")
        ));
    }

    @Transactional
    public Map<String,Object> updateConfiguration(ConfigurationRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Configuration is required");
        String current = value("STATUS", "DISABLED").toUpperCase(Locale.ROOT);
        String requested = defaultText(request.status(), request.enabled() == null ? current : (request.enabled() ? "ENABLED" : "DISABLED")).toUpperCase(Locale.ROOT);
        if (!Set.of("ENABLED", "DISABLING", "DISABLED").contains(requested)) throw new IllegalArgumentException("Invalid resource planning status");
        int open = count("SELECT COUNT(*) FROM funeral_resource_plan WHERE status NOT IN ('COMPLETED','CANCELLED')");
        if ("DISABLED".equals(requested) && open > 0 && !Boolean.TRUE.equals(request.disableImmediately())) {
            requested = "DISABLING";
        }
        if ("DISABLED".equals(requested) && open > 0) disableImmediately(userId);
        put("STATUS", requested);
        put("AUTO-CREATE-PLANS", request.autoCreatePlans(), true);
        put("REQUIRE-READINESS", request.requireReadiness(), true);
        put("PROVISIONAL-EXPIRY-HOURS", Math.max(1, request.provisionalExpiryHours() == null ? 2 : request.provisionalExpiryHours()));
        put("ALLOW-THIRD-PARTY-LEASING", request.allowThirdPartyLeasing(), true);
        put("REQUIRE-APPROVED-PO", request.requireApprovedPo(), true);
        put("DEFAULT-ASSIGNED-EMPLOYEE-ID", blank(request.defaultAssignedEmployeeId()));
        put("DEFAULT-ASSIGNED-ROLE-ID", blank(request.defaultAssignedRoleId()));
        int createdPlans = "ENABLED".equals(requested) ? synchronizeMissingPlans(userId) : 0;
        Map<String,Object> result = configuration();
        result.put("openPlans", count("SELECT COUNT(*) FROM funeral_resource_plan WHERE status NOT IN ('COMPLETED','CANCELLED')"));
        result.put("createdPlans", createdPlans);
        return result;
    }

    public boolean acceptsNewPlans() { return "ENABLED".equalsIgnoreCase(value("STATUS", "DISABLED")); }

    public List<Map<String,Object>> requirements(String packageId) {
        return jdbc.queryForList("SELECT r.*,p.code product_code,p.description product_description FROM funeral_resource_requirement r LEFT JOIN product p ON p.id=r.product_id WHERE r.funeral_package_id=? ORDER BY r.active DESC,r.mandatory DESC,r.name", packageId);
    }

    @Transactional
    public Map<String,Object> saveRequirement(String packageId, String id, RequirementRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Requirement is required");
        String type=required(request.resourceType(),"resourceType").toUpperCase(Locale.ROOT);
        if(!Set.of("ASSET","ASSET_POOL","VEHICLE","EMPLOYEE","STOCK").contains(type)) throw new IllegalArgumentException("Unsupported resourceType");
        String sourcing=defaultText(request.sourcingMode(),"INTERNAL_OR_EXTERNAL").toUpperCase(Locale.ROOT);
        if(!Set.of("INTERNAL_ONLY","EXTERNAL_ONLY","INTERNAL_OR_EXTERNAL").contains(sourcing)) throw new IllegalArgumentException("Unsupported sourcingMode");
        BigDecimal quantity=positive(request.quantity());
        int start=request.startOffsetMinutes()==null?0:request.startOffsetMinutes(); int end=request.endOffsetMinutes()==null?1440:request.endOffsetMinutes();
        if(end<=start) throw new IllegalArgumentException("endOffsetMinutes must be after startOffsetMinutes");
        String requirementId=StringUtils.hasText(id)?id:UUID.randomUUID().toString();
        if(StringUtils.hasText(id)) jdbc.update("UPDATE funeral_resource_requirement SET name=?,resource_type=?,resource_category=?,product_id=?,quantity=?,uom=?,mandatory=?,sourcing_mode=?,start_offset_minutes=?,end_offset_minutes=?,preferred_supplier_partner_id=?,active=?,notes=?,updated_by=? WHERE id=? AND funeral_package_id=?",required(request.name(),"name"),type,blank(request.resourceCategory()),blank(request.productId()),quantity,defaultText(request.uom(),"EA"),request.mandatory()==null||request.mandatory(),sourcing,start,end,blank(request.preferredSupplierPartnerId()),request.active()==null||request.active(),blank(request.notes()),userId,id,packageId);
        else jdbc.update("INSERT INTO funeral_resource_requirement(id,funeral_package_id,name,resource_type,resource_category,product_id,quantity,uom,mandatory,sourcing_mode,start_offset_minutes,end_offset_minutes,preferred_supplier_partner_id,active,notes,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",requirementId,packageId,required(request.name(),"name"),type,blank(request.resourceCategory()),blank(request.productId()),quantity,defaultText(request.uom(),"EA"),request.mandatory()==null||request.mandatory(),sourcing,start,end,blank(request.preferredSupplierPartnerId()),request.active()==null||request.active(),blank(request.notes()),userId,userId);
        return jdbc.queryForMap("SELECT * FROM funeral_resource_requirement WHERE id=?",requirementId);
    }

    @Transactional
    public void ensurePlan(String funeralServiceId, String userId) {
        if (!acceptsNewPlans() || !bool("AUTO-CREATE-PLANS", true) || !StringUtils.hasText(funeralServiceId)) return;
        List<Map<String,Object>> funeral = jdbc.queryForList("SELECT id,package_id,funeral_date,service_request_no FROM funeral_service WHERE id=?", funeralServiceId);
        if (funeral.isEmpty() || funeral.get(0).get("funeral_date") == null) return;
        Map<String,Object> f = funeral.get(0);
        List<String> plans = jdbc.queryForList("SELECT id FROM funeral_resource_plan WHERE funeral_service_id=?", String.class, funeralServiceId);
        String planId;
        if (plans.isEmpty()) {
            planId = UUID.randomUUID().toString();
            jdbc.update("INSERT INTO funeral_resource_plan(id,funeral_service_id,status,assigned_employee_id,assigned_role_id,created_by,updated_by) VALUES(?,?,'PLANNING_REQUIRED',?,?,?,?)",
                    planId, funeralServiceId, blank(value("DEFAULT-ASSIGNED-EMPLOYEE-ID", "")), blank(value("DEFAULT-ASSIGNED-ROLE-ID", "")), userId, userId);
        } else planId = plans.get(0);
        LocalDateTime base = ((java.sql.Date) f.get("funeral_date")).toLocalDate().atStartOfDay();
        List<Map<String,Object>> requirements = jdbc.queryForList("SELECT * FROM funeral_resource_requirement WHERE funeral_package_id=? AND active=1", f.get("package_id"));
        for (Map<String,Object> r : requirements) {
            String requirementId = text(r.get("id"));
            if (count("SELECT COUNT(*) FROM funeral_resource_plan_item WHERE resource_plan_id=? AND source_requirement_id=?", planId, requirementId) > 0) continue;
            jdbc.update("INSERT INTO funeral_resource_plan_item(id,resource_plan_id,source_requirement_id,name,resource_type,resource_category,product_id,required_quantity,uom,mandatory,sourcing_mode,start_at,end_at,status,notes,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,'UNPLANNED',?,?,?)",
                    UUID.randomUUID().toString(), planId, requirementId, r.get("name"), r.get("resource_type"), r.get("resource_category"), r.get("product_id"), r.get("quantity"), r.get("uom"), r.get("mandatory"), r.get("sourcing_mode"),
                    Timestamp.valueOf(base.plusMinutes(asInt(r.get("start_offset_minutes"),0))), Timestamp.valueOf(base.plusMinutes(asInt(r.get("end_offset_minutes"),1440))), r.get("notes"), userId, userId);
        }
    }

    public List<Map<String,Object>> plans(String status, String assignee, String query) {
        if (acceptsNewPlans() && bool("AUTO-CREATE-PLANS", true)) {
            synchronizeMissingPlans("SYSTEM");
        }
        expireProvisional();
        StringBuilder sql = new StringBuilder("SELECT rp.*,fs.service_request_no,fs.deceased_name,fs.funeral_date,fs.funeral_area,COUNT(i.id) item_count,SUM(CASE WHEN i.mandatory=1 AND i.status NOT IN ('COVERED','CONFIRMED','ORDERED','READY','COMPLETED') THEN 1 ELSE 0 END) unresolved_count FROM funeral_resource_plan rp JOIN funeral_service fs ON fs.id=rp.funeral_service_id LEFT JOIN funeral_resource_plan_item i ON i.resource_plan_id=rp.id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) { sql.append(" AND rp.status=?"); args.add(status.toUpperCase(Locale.ROOT)); }
        if (StringUtils.hasText(assignee)) { sql.append(" AND rp.assigned_employee_id=?"); args.add(assignee); }
        if (StringUtils.hasText(query)) { sql.append(" AND (UPPER(fs.service_request_no) LIKE ? OR UPPER(fs.deceased_name) LIKE ?)"); String q="%"+query.toUpperCase(Locale.ROOT)+"%"; args.add(q); args.add(q); }
        sql.append(" GROUP BY rp.id,fs.service_request_no,fs.deceased_name,fs.funeral_date,fs.funeral_area ORDER BY fs.funeral_date,rp.created_at");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public int synchronizeMissingPlans(String userId) {
        if (!acceptsNewPlans() || !bool("AUTO-CREATE-PLANS", true)) return 0;
        List<String> missingPlans = jdbc.queryForList("""
                SELECT fs.id
                  FROM funeral_service fs
             LEFT JOIN funeral_resource_plan rp ON rp.funeral_service_id=fs.id
                 WHERE rp.id IS NULL
                   AND fs.funeral_date IS NOT NULL
                   AND fs.funeral_date >= CURRENT_DATE
                   AND UPPER(COALESCE(fs.status, '')) <> 'CANCELLED'
                """, String.class);
        for (String funeralServiceId : missingPlans) {
            ensurePlan(funeralServiceId, userId);
        }
        return missingPlans.size();
    }

    public Map<String,Object> plan(String id) {
        Map<String,Object> plan = jdbc.queryForMap("SELECT rp.*,fs.service_request_no,fs.deceased_name,fs.funeral_date,fs.funeral_area FROM funeral_resource_plan rp JOIN funeral_service fs ON fs.id=rp.funeral_service_id WHERE rp.id=?", id);
        List<Map<String,Object>> items = jdbc.queryForList("SELECT * FROM funeral_resource_plan_item WHERE resource_plan_id=? ORDER BY mandatory DESC,start_at,name", id);
        for (Map<String,Object> item : items) item.put("allocations", jdbc.queryForList("SELECT a.*,ar.asset_no,ar.name asset_name,TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) partner_name,po.purchase_order_no,po.status purchase_order_status FROM funeral_resource_allocation a LEFT JOIN asset_register ar ON ar.id=a.asset_id LEFT JOIN partner p ON p.id=COALESCE(a.employee_partner_id,a.supplier_partner_id) LEFT JOIN purchase_order po ON po.id=a.purchase_order_id WHERE a.plan_item_id=? ORDER BY a.created_at", item.get("id")));
        plan.put("items", items);
        return plan;
    }

    @Transactional
    public Map<String,Object> assign(String planId, AssignmentRequest request, String userId) {
        jdbc.update("UPDATE funeral_resource_plan SET assigned_employee_id=?,assigned_role_id=?,urgent=?,special_instructions=?,updated_by=? WHERE id=?",blank(request.assignedEmployeeId()),blank(request.assignedRoleId()),Boolean.TRUE.equals(request.urgent()),blank(request.specialInstructions()),userId,planId);
        return plan(planId);
    }

    public List<Map<String,Object>> availableAssets(String itemId) {
        Map<String,Object> item = item(itemId);
        return jdbc.queryForList("SELECT a.*,COALESCE(pal.capacity,1)-COALESCE((SELECT SUM(x.quantity) FROM funeral_resource_allocation x JOIN funeral_resource_plan_item xi ON xi.id=x.plan_item_id WHERE x.asset_id=a.id AND x.status IN ('PROVISIONAL','CONFIRMED','ISSUED') AND xi.start_at<? AND xi.end_at>?),0) available_quantity FROM asset_register a LEFT JOIN product_asset_link pal ON pal.asset_id=a.id AND pal.service_product_id=? AND pal.active=1 WHERE a.status='ACTIVE' AND a.condition_status NOT IN ('DAMAGED','POOR','LOST') AND (? IS NULL OR UPPER(a.category)=UPPER(?)) HAVING available_quantity>0 ORDER BY a.name,a.asset_no",
                item.get("end_at"), item.get("start_at"), item.get("product_id"), item.get("resource_category"), item.get("resource_category"));
    }

    @Transactional
    public Map<String,Object> allocateInternal(String itemId, InternalAllocationRequest request, String userId) {
        Map<String,Object> item = lockItem(itemId);
        BigDecimal qty = positive(request.quantity());
        String type = text(item.get("resource_type")).toUpperCase(Locale.ROOT);
        String assetId = type.equals("EMPLOYEE") ? null : required(request.assetId(), "assetId");
        String employeeId = type.equals("EMPLOYEE") ? required(request.employeePartnerId(), "employeePartnerId") : null;
        String resourceColumn = type.equals("EMPLOYEE") ? "employee_partner_id" : "asset_id";
        String resourceId = type.equals("EMPLOYEE") ? employeeId : assetId;
        BigDecimal conflicting = jdbc.queryForObject("SELECT COALESCE(SUM(a.quantity),0) FROM funeral_resource_allocation a JOIN funeral_resource_plan_item i ON i.id=a.plan_item_id WHERE a."+resourceColumn+"=? AND a.status IN ('PROVISIONAL','CONFIRMED','ISSUED') AND i.start_at<? AND i.end_at>?", BigDecimal.class, resourceId, item.get("end_at"), item.get("start_at"));
        BigDecimal capacity=BigDecimal.ONE;
        if(!type.equals("EMPLOYEE")){List<Integer> capacities=jdbc.queryForList("SELECT capacity FROM product_asset_link WHERE asset_id=? AND service_product_id=? AND active=1",Integer.class,assetId,item.get("product_id"));if(!capacities.isEmpty())capacity=BigDecimal.valueOf(capacities.get(0));}
        if ((conflicting==null?BigDecimal.ZERO:conflicting).add(qty).compareTo(capacity)>0) throw new IllegalArgumentException("Selected resource does not have sufficient capacity during this funeral window");
        LocalDateTime expiry = LocalDateTime.now().plusHours(integer("PROVISIONAL-EXPIRY-HOURS",2));
        jdbc.update("INSERT INTO funeral_resource_allocation(id,plan_item_id,allocation_type,asset_id,employee_partner_id,quantity,status,expires_at,notes,created_by,updated_by) VALUES(?,?,?,?,?,?,'PROVISIONAL',?,?,?,?)",
                UUID.randomUUID().toString(), itemId, type, assetId, employeeId, qty, Timestamp.valueOf(expiry), blank(request.notes()), userId, userId);
        recalculate(itemId, userId);
        return plan(text(item.get("resource_plan_id")));
    }

    @Transactional
    public Map<String,Object> leaseExternal(String itemId, ExternalAllocationRequest request, String userId) {
        if (!bool("ALLOW-THIRD-PARTY-LEASING",true)) throw new IllegalStateException("Third-party leasing is disabled for this tenant");
        Map<String,Object> item = lockItem(itemId);
        String supplier = required(request.supplierPartnerId(), "supplierPartnerId");
        String product = required(defaultText(request.productId(), text(item.get("product_id"))), "productId");
        BigDecimal qty = positive(request.quantity());
        BigDecimal unit = request.unitCost()==null ? BigDecimal.ZERO : request.unitCost();
        String planId=text(item.get("resource_plan_id"));
        Map<String,Object> header=jdbc.queryForMap("SELECT rp.funeral_service_id,fs.service_request_no,fs.funeral_date FROM funeral_resource_plan rp JOIN funeral_service fs ON fs.id=rp.funeral_service_id WHERE rp.id=?",planId);
        List<String> existing=jdbc.queryForList("SELECT DISTINCT a.purchase_order_id FROM funeral_resource_allocation a JOIN funeral_resource_plan_item i ON i.id=a.plan_item_id JOIN purchase_order po ON po.id=a.purchase_order_id WHERE i.resource_plan_id=? AND a.supplier_partner_id=? AND po.status='DRAFT'",String.class,planId,supplier);
        String poId=existing.isEmpty()?UUID.randomUUID().toString():existing.get(0);
        if(existing.isEmpty()) jdbc.update("INSERT INTO purchase_order(id,purchase_order_no,supplier_partner_id,order_date,expected_delivery_date,status,currency,subtotal_amount,tax_amount,total_amount,notes,created_at,created_by,updated_at,updated_by) VALUES(?,?,?,CURRENT_DATE,?,'DRAFT','ZAR',0,0,0,?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,?)",
                poId,nextPo(),supplier,header.get("funeral_date"),"Funeral resource lease for "+header.get("service_request_no"),userId,userId);
        String lineId=UUID.randomUUID().toString(); int lineNo=10+count("SELECT COUNT(*) FROM purchase_order_line WHERE purchase_order_id=?",poId)*10;
        BigDecimal subtotal=unit.multiply(qty); BigDecimal tax=subtotal.multiply(new BigDecimal("0.15"));
        jdbc.update("INSERT INTO purchase_order_line(id,purchase_order_id,line_no,product_id,product_description,ordered_qty,received_qty,open_qty,uom,unit_cost,tax_rate,line_subtotal,line_tax,line_total,status,notes,created_at,created_by) VALUES(?,?,?,?,?,?,0,?,?,?,15,?,?,?,'OPEN',?,CURRENT_TIMESTAMP,?)",
                lineId,poId,lineNo,product,item.get("name"),qty,qty,item.get("uom"),unit,subtotal,tax,subtotal.add(tax),"External funeral resource lease",userId);
        jdbc.update("UPDATE purchase_order SET subtotal_amount=subtotal_amount+?,tax_amount=tax_amount+?,total_amount=total_amount+?,updated_at=CURRENT_TIMESTAMP,updated_by=? WHERE id=?",subtotal,tax,subtotal.add(tax),userId,poId);
        jdbc.update("INSERT INTO funeral_resource_allocation(id,plan_item_id,allocation_type,supplier_partner_id,quantity,status,purchase_order_id,purchase_order_line_id,unit_cost,notes,created_by,updated_by) VALUES(?,?,'EXTERNAL',?,?,'DRAFT_PO',?,?,?,?,?,?)",
                UUID.randomUUID().toString(),itemId,supplier,qty,poId,lineId,unit,blank(request.notes()),userId,userId);
        recalculate(itemId,userId); return plan(planId);
    }

    @Transactional
    public Map<String,Object> confirmReady(String planId, String userId) {
        jdbc.queryForMap("SELECT id FROM funeral_resource_plan WHERE id=? FOR UPDATE",planId);
        int unresolved=count("SELECT COUNT(*) FROM funeral_resource_plan_item WHERE resource_plan_id=? AND mandatory=1 AND allocated_quantity<required_quantity",planId);
        if(unresolved>0) throw new IllegalStateException("All mandatory resources must be covered before the plan can be marked ready");
        if(bool("REQUIRE-APPROVED-PO",true) && count("SELECT COUNT(*) FROM funeral_resource_allocation a JOIN funeral_resource_plan_item i ON i.id=a.plan_item_id JOIN purchase_order po ON po.id=a.purchase_order_id WHERE i.resource_plan_id=? AND a.allocation_type='EXTERNAL' AND po.status NOT IN ('APPROVED','SENT','CONFIRMED','PARTIAL','RECEIVED','COMPLETED')",planId)>0)
            throw new IllegalStateException("All external-resource purchase orders must be approved before readiness");
        jdbc.update("UPDATE funeral_resource_allocation a JOIN funeral_resource_plan_item i ON i.id=a.plan_item_id SET a.status=CASE WHEN a.status='PROVISIONAL' THEN 'CONFIRMED' ELSE a.status END,a.expires_at=NULL,a.updated_by=? WHERE i.resource_plan_id=?",userId,planId);
        jdbc.update("UPDATE funeral_resource_plan_item SET status='READY',updated_by=? WHERE resource_plan_id=?",userId,planId);
        jdbc.update("UPDATE funeral_resource_plan SET status='READY',ready_at=CURRENT_TIMESTAMP,updated_by=? WHERE id=?",userId,planId);
        return plan(planId);
    }

    @Transactional public void cancelForFuneral(String funeralServiceId,String userId){
        jdbc.update("UPDATE funeral_resource_allocation a JOIN funeral_resource_plan_item i ON i.id=a.plan_item_id JOIN funeral_resource_plan p ON p.id=i.resource_plan_id SET a.status='CANCELLED',a.updated_by=? WHERE p.funeral_service_id=? AND a.status IN ('PROVISIONAL','CONFIRMED')",userId,funeralServiceId);
        jdbc.update("UPDATE funeral_resource_plan SET status='CANCELLED',updated_by=? WHERE funeral_service_id=?",userId,funeralServiceId);
    }

    private void disableImmediately(String userId){ jdbc.update("UPDATE funeral_resource_allocation SET status='CANCELLED',updated_by=? WHERE status IN ('PROVISIONAL','CONFIRMED')",userId); jdbc.update("UPDATE funeral_resource_plan SET status='CANCELLED',updated_by=? WHERE status NOT IN ('COMPLETED','CANCELLED')",userId); jdbc.update("UPDATE purchase_order po JOIN funeral_resource_allocation a ON a.purchase_order_id=po.id SET po.status='CANCELLED',po.updated_by=? WHERE po.status='DRAFT'",userId); }
    private void expireProvisional(){ jdbc.update("UPDATE funeral_resource_allocation SET status='EXPIRED' WHERE status='PROVISIONAL' AND expires_at<CURRENT_TIMESTAMP"); }
    private Map<String,Object> item(String id){return jdbc.queryForMap("SELECT * FROM funeral_resource_plan_item WHERE id=?",id);}
    private Map<String,Object> lockItem(String id){return jdbc.queryForMap("SELECT * FROM funeral_resource_plan_item WHERE id=? FOR UPDATE",id);}
    private void recalculate(String itemId,String userId){jdbc.update("UPDATE funeral_resource_plan_item i SET allocated_quantity=(SELECT COALESCE(SUM(a.quantity),0) FROM funeral_resource_allocation a WHERE a.plan_item_id=i.id AND a.status IN ('PROVISIONAL','CONFIRMED','ISSUED','ORDERED')),status=CASE WHEN (SELECT COALESCE(SUM(a.quantity),0) FROM funeral_resource_allocation a WHERE a.plan_item_id=i.id AND a.status IN ('PROVISIONAL','CONFIRMED','ISSUED','ORDERED'))>=i.required_quantity THEN 'COVERED' ELSE 'IN_PROGRESS' END,updated_by=? WHERE i.id=?",userId,itemId); jdbc.update("UPDATE funeral_resource_plan p JOIN funeral_resource_plan_item i ON i.resource_plan_id=p.id SET p.status='IN_PROGRESS',p.updated_by=? WHERE i.id=? AND p.status='PLANNING_REQUIRED'",userId,itemId);}
    private String nextPo(){try{return numbers.generateNumber("PURCHASE_ORDER");}catch(Exception e){return "PO-"+System.currentTimeMillis();}}
    private int count(String sql,Object...args){Integer n=jdbc.queryForObject(sql,Integer.class,args);return n==null?0:n;}
    private String value(String key,String fallback){String v=settings.getSetting(key,GROUP);return StringUtils.hasText(v)?v:fallback;}
    private boolean bool(String key,boolean fallback){String v=value(key,String.valueOf(fallback));return "true".equalsIgnoreCase(v)||"1".equals(v)||"yes".equalsIgnoreCase(v);}
    private int integer(String key,int fallback){try{return Integer.parseInt(value(key,String.valueOf(fallback)));}catch(Exception e){return fallback;}}
    private void put(String key,Object value){settings.upsertSetting(key,GROUP,value==null?"":String.valueOf(value));}
    private void put(String key,Boolean value,boolean fallback){put(key,value==null?fallback:value);}
    private int asInt(Object v,int fallback){try{return Integer.parseInt(String.valueOf(v));}catch(Exception e){return fallback;}}
    private BigDecimal positive(BigDecimal v){if(v==null||v.compareTo(BigDecimal.ZERO)<=0)throw new IllegalArgumentException("quantity must be greater than zero");return v;}
    private String required(String v,String name){if(!StringUtils.hasText(v))throw new IllegalArgumentException(name+" is required");return v.trim();}
    private String blank(String v){return StringUtils.hasText(v)?v.trim():null;}
    private String text(Object v){return v==null?"":String.valueOf(v);}
    private String defaultText(String v,String d){return StringUtils.hasText(v)?v.trim():d;}

    public record ConfigurationRequest(Boolean enabled,String status,Boolean autoCreatePlans,Boolean requireReadiness,Integer provisionalExpiryHours,Boolean allowThirdPartyLeasing,Boolean requireApprovedPo,String defaultAssignedEmployeeId,String defaultAssignedRoleId,Boolean disableImmediately){}
    public record InternalAllocationRequest(String assetId,String employeePartnerId,BigDecimal quantity,String notes){}
    public record ExternalAllocationRequest(String supplierPartnerId,String productId,BigDecimal quantity,BigDecimal unitCost,String notes){}
    public record RequirementRequest(String name,String resourceType,String resourceCategory,String productId,BigDecimal quantity,String uom,Boolean mandatory,String sourcingMode,Integer startOffsetMinutes,Integer endOffsetMinutes,String preferredSupplierPartnerId,Boolean active,String notes){}
    public record AssignmentRequest(String assignedEmployeeId,String assignedRoleId,Boolean urgent,String specialInstructions){}
}
