package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderLineRequest;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderRequest;
import za.co.mawa.bes.dto.v2.serviceorder.ServiceOrderResponse;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SupplierNetworkService {
    private static final Pattern TENANT = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private static final Set<String> RESPONSES = Set.of("ACCEPTED", "PARTIALLY_ACCEPTED", "CHANGE_PROPOSED", "DECLINED");
    private static final Set<String> ACTIVE_RESERVATIONS = Set.of("RESERVED", "ISSUED", "IN_PROGRESS");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ServiceOrderService serviceOrders;
    private final SettingService settings;
    private final TenantAdminService tenants;

    public SupplierNetworkService(JdbcTemplate jdbc, ObjectMapper json, ServiceOrderService serviceOrders,
                                  SettingService settings, TenantAdminService tenants) {
        this.jdbc = jdbc;
        this.json = json;
        this.serviceOrders = serviceOrders;
        this.settings = settings;
        this.tenants = tenants;
    }

    public Map<String,Object> configuration() {
        boolean enabled="ENABLED".equalsIgnoreCase(settings.getSetting("STATUS","MAWA-SUPPLIER-NETWORK"));
        return Map.of("status",enabled?"ENABLED":"DISABLED","enabled",enabled);
    }

    @Transactional
    public Map<String,Object> updateConfiguration(ConfigurationRequest request) {
        if(request==null||request.enabled()==null) throw new IllegalArgumentException("enabled is required");
        settings.upsertSetting("STATUS","MAWA-SUPPLIER-NETWORK",request.enabled()?"ENABLED":"DISABLED");
        return configuration();
    }

    public List<Map<String,Object>> tenantOptions(){
        String current=TenantContext.getCurrentTenant();
        return tenants.getAll().stream().filter(Objects::nonNull)
                .filter(t->StringUtils.hasText(t.getId())&&!t.getId().equals(current))
                .filter(t->t.getStatus()==null||"ACTIVE".equalsIgnoreCase(String.valueOf(t.getStatus())))
                .sorted(Comparator.comparing(t->defaultText(t.getName(),t.getId()),String.CASE_INSENSITIVE_ORDER))
                .map(t->{Map<String,Object>x=new LinkedHashMap<>();x.put("id",t.getId());x.put("name",defaultText(t.getName(),t.getId()));x.put("host",t.getHost());return x;}).toList();
    }

    public List<Map<String,Object>> resourceOptions(String type,String query){
        requireEnabled(); String q="%"+defaultText(query,"").toUpperCase(Locale.ROOT)+"%";
        if("EMPLOYEE".equalsIgnoreCase(type)) return jdbc.queryForList("SELECT DISTINCT p.id,TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) name,p.number FROM partner p JOIN partner_role pr ON pr.partner=p.id WHERE pr.role='EMPLOYEE' AND p.status='ACTIVE' AND (UPPER(p.number) LIKE ? OR UPPER(CONCAT_WS(' ',p.name2,p.name3,p.name1)) LIKE ?) ORDER BY name LIMIT 100",q,q);
        return jdbc.queryForList("SELECT id,asset_no number,name,category FROM asset_register WHERE status='ACTIVE' AND condition_status NOT IN ('DAMAGED','POOR','LOST') AND (UPPER(asset_no) LIKE ? OR UPPER(name) LIKE ? OR UPPER(COALESCE(category,'')) LIKE ?) ORDER BY name LIMIT 100",q,q,q);
    }

    public List<Map<String,Object>> connections() {
        requireEnabled();
        return jdbc.queryForList("SELECT c.*,TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) local_partner_name FROM tenant_trading_connection c LEFT JOIN partner p ON p.id=c.local_partner_id ORDER BY c.updated_at DESC,c.created_at DESC");
    }

    @Transactional
    public Map<String,Object> saveConnection(ConnectionRequest r, String user) {
        requireEnabled();
        if (r == null) throw new IllegalArgumentException("Connection is required");
        String remote = tenant(r.remoteTenantId());
        String type = required(r.relationshipType(), "relationshipType").toUpperCase(Locale.ROOT);
        if (!Set.of("BUYER", "SUPPLIER").contains(type)) throw new IllegalArgumentException("relationshipType must be BUYER or SUPPLIER");
        String status = defaultText(r.status(), "PENDING").toUpperCase(Locale.ROOT);
        if (!Set.of("PENDING","ACTIVE","SUSPENDED","REJECTED","ENDED").contains(status)) throw new IllegalArgumentException("Invalid connection status");
        String id = StringUtils.hasText(r.id()) ? r.id().trim() : UUID.randomUUID().toString();
        jdbc.update("INSERT INTO tenant_trading_connection(id,remote_tenant_id,local_partner_id,remote_partner_id,relationship_type,status,allow_purchase_orders,allow_invoices,effective_from,effective_to,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE local_partner_id=VALUES(local_partner_id),remote_partner_id=VALUES(remote_partner_id),status=VALUES(status),allow_purchase_orders=VALUES(allow_purchase_orders),allow_invoices=VALUES(allow_invoices),effective_from=VALUES(effective_from),effective_to=VALUES(effective_to),updated_by=VALUES(updated_by)",
                id,remote,required(r.localPartnerId(),"localPartnerId"),blank(r.remotePartnerId()),type,status,r.allowPurchaseOrders()==null||r.allowPurchaseOrders(),r.allowInvoices()==null||r.allowInvoices(),r.effectiveFrom()==null? LocalDate.now():r.effectiveFrom(),r.effectiveTo(),actor(user),actor(user));
        return jdbc.queryForMap("SELECT * FROM tenant_trading_connection WHERE remote_tenant_id=? AND relationship_type=?",remote,type);
    }

    @Transactional
    public Map<String,Object> sendPurchaseOrder(String poId, String user) {
        requireEnabled();
        String buyerTenant=tenant(TenantContext.getCurrentTenant());
        Map<String,Object> po=one("SELECT po.*,p.number supplier_no,TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) supplier_name FROM purchase_order po LEFT JOIN partner p ON p.id=po.supplier_partner_id WHERE po.id=?",poId);
        String poStatus=text(po.get("status")).toUpperCase(Locale.ROOT);
        if(!Set.of("APPROVED","SENT","CHANGE_PROPOSED").contains(poStatus)) throw new IllegalStateException("Purchase order must be approved before it is sent");
        Map<String,Object> connection=one("SELECT * FROM tenant_trading_connection WHERE local_partner_id=? AND relationship_type='SUPPLIER' AND status='ACTIVE' AND allow_purchase_orders=1 AND effective_from<=CURRENT_DATE AND (effective_to IS NULL OR effective_to>=CURRENT_DATE)",po.get("supplier_partner_id"));
        String supplierTenant=tenant(text(connection.get("remote_tenant_id")));
        Map<String,Object> reciprocal=one("SELECT * FROM "+table(supplierTenant,"tenant_trading_connection")+" WHERE remote_tenant_id=? AND relationship_type='BUYER' AND status='ACTIVE' AND allow_purchase_orders=1",buyerTenant);
        String exchangeKey="PO:"+buyerTenant+":"+poId;
        List<Map<String,Object>> lines=jdbc.queryForList("SELECT * FROM purchase_order_line WHERE purchase_order_id=? ORDER BY line_no",poId);
        String orderId=UUID.randomUUID().toString();
        List<Map<String,Object>> existing=jdbc.queryForList("SELECT id,order_no FROM "+table(supplierTenant,"supplier_customer_order")+" WHERE exchange_key=?",exchangeKey);
        if(existing.isEmpty()) {
            String orderNo="MCO-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
            String funeralRef=funeralReference(poId);
            jdbc.update("INSERT INTO "+table(supplierTenant,"supplier_customer_order")+"(id,order_no,exchange_key,buyer_tenant_id,customer_partner_id,buyer_purchase_order_id,buyer_purchase_order_no,funeral_reference,status,scheduled_start_at,scheduled_end_at,currency,subtotal_amount,tax_amount,total_amount,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?, 'RECEIVED',?,DATE_ADD(?,INTERVAL 1 DAY),?,?,?,?,?,?)",
                    orderId,orderNo,exchangeKey,buyerTenant,reciprocal.get("local_partner_id"),poId,po.get("purchase_order_no"),funeralRef,po.get("expected_delivery_date"),po.get("expected_delivery_date"),po.get("currency"),po.get("subtotal_amount"),po.get("tax_amount"),po.get("total_amount"),actor(user),actor(user));
            for(Map<String,Object> l:lines) jdbc.update("INSERT INTO "+table(supplierTenant,"supplier_customer_order_line")+"(id,customer_order_id,buyer_purchase_order_line_id,line_no,product_id,description,requested_quantity,confirmed_quantity,uom,unit_price,tax_rate,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,'PENDING')",
                    UUID.randomUUID().toString(),orderId,l.get("id"),l.get("line_no"),null,defaultText(text(l.get("product_description")),"Service"),l.get("ordered_qty"),BigDecimal.ZERO,l.get("uom"),l.get("unit_cost"),l.get("tax_rate"));
        } else orderId=text(existing.get(0).get("id"));
        String payload=toJson(Map.of("purchaseOrder",po,"lines",lines));
        upsertExchange(exchangeKey,"PURCHASE_ORDER","OUTBOUND",supplierTenant,poId,orderId,text(po.get("purchase_order_no")),null,"DELIVERED",payload);
        jdbc.update("INSERT INTO "+table(supplierTenant,"trading_document_exchange")+"(id,exchange_key,document_type,direction,remote_tenant_id,local_document_id,remote_document_id,remote_document_no,status,payload_json) VALUES(?,?,'PURCHASE_ORDER','INBOUND',?,?,?,?,'RECEIVED',CAST(? AS JSON)) ON DUPLICATE KEY UPDATE status='RECEIVED',payload_json=VALUES(payload_json)",UUID.randomUUID().toString(),exchangeKey,buyerTenant,orderId,poId,po.get("purchase_order_no"),payload);
        jdbc.update("UPDATE purchase_order SET status='SENT',updated_at=CURRENT_TIMESTAMP,updated_by=? WHERE id=?",actor(user),poId);
        jdbc.update("UPDATE funeral_resource_allocation SET status='AWAITING_SUPPLIER',supplier_exchange_key=?,supplier_customer_order_id=?,updated_by=? WHERE purchase_order_id=?",exchangeKey,orderId,actor(user),poId);
        return getExchange(exchangeKey);
    }

    public List<Map<String,Object>> orders(String status) {
        requireEnabled();
        String sql="SELECT o.*,TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) customer_name FROM supplier_customer_order o LEFT JOIN partner p ON p.id=o.customer_partner_id";
        List<Map<String,Object>> rows=StringUtils.hasText(status)&&!"ALL".equalsIgnoreCase(status)?jdbc.queryForList(sql+" WHERE o.status=? ORDER BY o.created_at DESC",status.toUpperCase(Locale.ROOT)):jdbc.queryForList(sql+" ORDER BY o.created_at DESC");
        return rows;
    }

    public Map<String,Object> order(String id) {
        requireEnabled();
        Map<String,Object> result=one("SELECT o.*,TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) customer_name FROM supplier_customer_order o LEFT JOIN partner p ON p.id=o.customer_partner_id WHERE o.id=?",id);
        result.put("lines",jdbc.queryForList("SELECT * FROM supplier_customer_order_line WHERE customer_order_id=? ORDER BY line_no",id));
        result.put("reservations",jdbc.queryForList("SELECT r.*,a.asset_no,a.name asset_name,TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) employee_name FROM supplier_resource_reservation r LEFT JOIN asset_register a ON a.id=r.asset_id LEFT JOIN partner p ON p.id=r.employee_partner_id WHERE r.customer_order_id=? ORDER BY r.start_at",id));
        return result;
    }

    @Transactional
    public Map<String,Object> respond(String id, ResponseRequest r, String user) {
        Map<String,Object> order=order(id);
        String response=required(r.status(),"status").toUpperCase(Locale.ROOT);
        if(!RESPONSES.contains(response)) throw new IllegalArgumentException("Unsupported response status");
        if(r.lines()!=null) for(LineResponse l:r.lines()) jdbc.update("UPDATE supplier_customer_order_line SET confirmed_quantity=?,unit_price=COALESCE(?,unit_price),status=?,proposed_note=? WHERE id=? AND customer_order_id=?",l.confirmedQuantity(),l.unitPrice(),defaultText(l.status(),response),blank(l.note()),l.id(),id);
        else if("ACCEPTED".equals(response)) jdbc.update("UPDATE supplier_customer_order_line SET confirmed_quantity=requested_quantity,status='ACCEPTED' WHERE customer_order_id=?",id);
        jdbc.update("UPDATE supplier_customer_order SET status=?,response_note=?,scheduled_start_at=COALESCE(?,scheduled_start_at),scheduled_end_at=COALESCE(?,scheduled_end_at),updated_by=? WHERE id=?",response,blank(r.note()),r.scheduledStartAt(),r.scheduledEndAt(),actor(user),id);
        if(Set.of("ACCEPTED","PARTIALLY_ACCEPTED").contains(response) && !StringUtils.hasText(text(order.get("service_order_id")))) {
            ServiceOrderResponse so=serviceOrders.create(toServiceOrder(order(id)),user);
            jdbc.update("UPDATE supplier_customer_order SET service_order_id=?,status=CASE WHEN status='ACCEPTED' THEN 'SCHEDULED' ELSE status END,updated_by=? WHERE id=?",so.getId(),actor(user),id);
        }
        updateBuyerFromResponse(order,response,r.note(),user);
        return order(id);
    }

    @Transactional
    public Map<String,Object> reserve(String orderId, ReservationRequest r, String user) {
        Map<String,Object> order=order(orderId);
        LocalDateTime start=r.startAt()!=null?r.startAt():timestamp(order.get("scheduled_start_at"));
        LocalDateTime end=r.endAt()!=null?r.endAt():timestamp(order.get("scheduled_end_at"));
        if(start==null||end==null||!end.isAfter(start)) throw new IllegalArgumentException("A valid reservation window is required");
        if(!StringUtils.hasText(r.assetId())&&!StringUtils.hasText(r.employeePartnerId())) throw new IllegalArgumentException("Select an asset or employee");
        BigDecimal qty=r.quantity()==null?BigDecimal.ONE:r.quantity();
        int clashes=jdbc.queryForObject("SELECT COUNT(*) FROM supplier_resource_reservation WHERE status IN ('RESERVED','ISSUED','IN_PROGRESS') AND ((asset_id IS NOT NULL AND asset_id=?) OR (employee_partner_id IS NOT NULL AND employee_partner_id=?)) AND start_at<? AND end_at>?",Integer.class,blank(r.assetId()),blank(r.employeePartnerId()),Timestamp.valueOf(end),Timestamp.valueOf(start));
        if(clashes>0) throw new IllegalStateException("The selected resource is already reserved during this period");
        jdbc.update("INSERT INTO supplier_resource_reservation(id,customer_order_id,customer_order_line_id,asset_id,employee_partner_id,quantity,start_at,end_at,status,notes,created_by) VALUES(?,?,?,?,?,?,?,?,'RESERVED',?,?)",UUID.randomUUID().toString(),orderId,required(r.lineId(),"lineId"),blank(r.assetId()),blank(r.employeePartnerId()),qty,Timestamp.valueOf(start),Timestamp.valueOf(end),blank(r.notes()),actor(user));
        return order(orderId);
    }

    @Transactional
    public Map<String,Object> complete(String id, String user) {
        Map<String,Object> o=order(id);
        if(!Set.of("SCHEDULED","IN_PROGRESS","PARTIALLY_ACCEPTED","ACCEPTED").contains(text(o.get("status")))) throw new IllegalStateException("Only accepted or scheduled orders can be completed");
        jdbc.update("UPDATE supplier_customer_order SET status='COMPLETED',updated_by=? WHERE id=?",actor(user),id);
        jdbc.update("UPDATE supplier_resource_reservation SET status='COMPLETED' WHERE customer_order_id=? AND status IN ('RESERVED','ISSUED','IN_PROGRESS')",id);
        updateBuyerFromResponse(o,"COMPLETED","Supplier confirmed service completion",user);
        return order(id);
    }

    @Transactional
    public Map<String,Object> invoice(String id, String user) {
        Map<String,Object> o=order(id);
        if(!"COMPLETED".equals(text(o.get("status")))) throw new IllegalStateException("Complete the customer order before invoicing");
        String serviceOrderId=required(text(o.get("service_order_id")),"serviceOrderId");
        InvoiceEntity invoice=serviceOrders.createInvoice(serviceOrderId,user);
        jdbc.update("UPDATE supplier_customer_order SET status='INVOICED',customer_invoice_id=?,updated_by=? WHERE id=?",invoice.getId(),actor(user),id);
        mirrorInvoiceToBuyer(o,invoice,user);
        return order(id);
    }

    private void updateBuyerFromResponse(Map<String,Object> o,String response,String note,String user){
        String buyer=tenant(text(o.get("buyer_tenant_id"))); String exchange=text(o.get("exchange_key")); String poId=text(o.get("buyer_purchase_order_id"));
        String poStatus=switch(response){case "ACCEPTED"->"CONFIRMED";case "PARTIALLY_ACCEPTED"->"PARTIAL";case "CHANGE_PROPOSED"->"CHANGE_PROPOSED";case "DECLINED"->"DECLINED";case "COMPLETED"->"COMPLETED";default->response;};
        jdbc.update("UPDATE "+table(buyer,"purchase_order")+" SET status=?,supplier_reference=?,updated_at=CURRENT_TIMESTAMP,updated_by=? WHERE id=?",poStatus,o.get("order_no"),actor(user),poId);
        String allocationStatus=switch(response){case "ACCEPTED"->"CONFIRMED";case "PARTIALLY_ACCEPTED"->"PARTIAL";case "COMPLETED"->"COMPLETED";default->response;};
        jdbc.update("UPDATE "+table(buyer,"funeral_resource_allocation")+" SET status=?,notes=CONCAT_WS(' | ',notes,?),updated_by=? WHERE supplier_exchange_key=?",allocationStatus,blank(note),actor(user),exchange);
        if (Set.of("ACCEPTED","PARTIALLY_ACCEPTED").contains(response)) {
            List<Map<String,Object>> confirmed=jdbc.queryForList("SELECT buyer_purchase_order_line_id,confirmed_quantity FROM supplier_customer_order_line WHERE customer_order_id=?",o.get("id"));
            for(Map<String,Object> line:confirmed) jdbc.update("UPDATE "+table(buyer,"funeral_resource_allocation")+" SET quantity=?,updated_by=? WHERE supplier_exchange_key=? AND purchase_order_line_id=?",line.get("confirmed_quantity"),actor(user),exchange,line.get("buyer_purchase_order_line_id"));
        }
        jdbc.update("UPDATE "+table(buyer,"funeral_resource_plan_item")+" i SET allocated_quantity=(SELECT COALESCE(SUM(a.quantity),0) FROM "+table(buyer,"funeral_resource_allocation")+" a WHERE a.plan_item_id=i.id AND a.status IN ('PROVISIONAL','CONFIRMED','ISSUED','ORDERED','PARTIAL','COMPLETED')),status=CASE WHEN (SELECT COALESCE(SUM(a.quantity),0) FROM "+table(buyer,"funeral_resource_allocation")+" a WHERE a.plan_item_id=i.id AND a.status IN ('PROVISIONAL','CONFIRMED','ISSUED','ORDERED','PARTIAL','COMPLETED'))>=i.required_quantity THEN 'COVERED' ELSE 'IN_PROGRESS' END,updated_by=? WHERE EXISTS(SELECT 1 FROM "+table(buyer,"funeral_resource_allocation")+" a WHERE a.plan_item_id=i.id AND a.supplier_exchange_key=?)",actor(user),exchange);
        jdbc.update("UPDATE "+table(buyer,"trading_document_exchange")+" SET status=?,remote_document_no=?,updated_at=CURRENT_TIMESTAMP WHERE exchange_key=?",response,o.get("order_no"),exchange);
    }

    private void mirrorInvoiceToBuyer(Map<String,Object> o,InvoiceEntity invoice,String user){
        String buyer=tenant(text(o.get("buyer_tenant_id"))); String paymentId=UUID.randomUUID().toString(); String requestNo="PR-MAWA-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
        Map<String,Object> buyerConnection=one("SELECT * FROM "+table(buyer,"tenant_trading_connection")+" WHERE remote_tenant_id=? AND relationship_type='SUPPLIER' AND status='ACTIVE' AND allow_invoices=1",tenant(TenantContext.getCurrentTenant()));
        String supplierPartner=text(buyerConnection.get("local_partner_id"));
        Map<String,Object> supplier=one("SELECT TRIM(CONCAT(COALESCE(name2,''),' ',COALESCE(name3,''),' ',COALESCE(name1,''))) name FROM "+table(buyer,"partner")+" WHERE id=?",supplierPartner);
        BigDecimal amount=BigDecimal.valueOf(invoice.getTotalCents()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        jdbc.update("INSERT INTO "+table(buyer,"payment_request")+"(id,request_no,request_type,source_type,source_id,payee_partner_id,payee_name,amount,currency,payment_method,invoice_no,external_reference,payment_reason,notes,requested_payment_date,status,created_by,updated_by) VALUES(?,?,'SUPPLIER_INVOICE','SUPPLIER_INVOICE',?,?,?,?,?,'EFT',?,?,?, ?,CURRENT_DATE,'DRAFT',?,?)",paymentId,requestNo,o.get("buyer_purchase_order_id"),supplierPartner,supplier.get("name"),amount,invoice.getCurrency(),invoice.getInvoiceNo(),o.get("buyer_purchase_order_no"),"MAWA supplier service invoice","Matched to MAWA PO; service completion confirmed",actor(user),actor(user));
        String invoiceExchange="INV:"+tenant(TenantContext.getCurrentTenant())+":"+invoice.getId();
        jdbc.update("INSERT INTO "+table(buyer,"trading_document_exchange")+"(id,exchange_key,document_type,direction,remote_tenant_id,local_document_id,remote_document_id,local_document_no,remote_document_no,status,payload_json) VALUES(?,?,'INVOICE','INBOUND',?,?,?,?,?,'RECEIVED',CAST(? AS JSON))",UUID.randomUUID().toString(),invoiceExchange,tenant(TenantContext.getCurrentTenant()),paymentId,invoice.getId(),requestNo,invoice.getInvoiceNo(),toJson(Map.of("invoiceId",invoice.getId(),"invoiceNo",invoice.getInvoiceNo(),"amount",amount,"purchaseOrderNo",o.get("buyer_purchase_order_no"))));
        jdbc.update("UPDATE supplier_customer_order SET status='INVOICED' WHERE id=?",o.get("id"));
    }

    private ServiceOrderRequest toServiceOrder(Map<String,Object> o){
        ServiceOrderRequest r=new ServiceOrderRequest(); r.setCustomerPartnerId(text(o.get("customer_partner_id"))); r.setOrderDate(LocalDate.now()); r.setStatus("SCHEDULED"); r.setScheduledStartAt(timestamp(o.get("scheduled_start_at"))); r.setScheduledEndAt(timestamp(o.get("scheduled_end_at"))); r.setNotes("MAWA customer PO "+o.get("buyer_purchase_order_no")+(StringUtils.hasText(text(o.get("funeral_reference")))?" · Funeral "+o.get("funeral_reference"):""));
        @SuppressWarnings("unchecked") List<Map<String,Object>> lines=(List<Map<String,Object>>)o.get("lines"); List<ServiceOrderLineRequest> result=new ArrayList<>();
        for(Map<String,Object> l:lines){if(decimal(l.get("confirmed_quantity")).signum()<=0)continue; ServiceOrderLineRequest x=new ServiceOrderLineRequest();x.setDescription(text(l.get("description")));x.setItemType("SERVICE");x.setQuantity(decimal(l.get("confirmed_quantity")).doubleValue());x.setUnitPriceCents(decimal(l.get("unit_price")).multiply(BigDecimal.valueOf(100)).longValue());x.setScheduledStartAt(r.getScheduledStartAt());x.setScheduledEndAt(r.getScheduledEndAt());result.add(x);} r.setLines(result); return r;
    }
    private String funeralReference(String poId){List<String>x=jdbc.queryForList("SELECT fs.service_request_no FROM funeral_resource_allocation a JOIN funeral_resource_plan_item i ON i.id=a.plan_item_id JOIN funeral_resource_plan rp ON rp.id=i.resource_plan_id JOIN funeral_service fs ON fs.id=rp.funeral_service_id WHERE a.purchase_order_id=? LIMIT 1",String.class,poId);return x.isEmpty()?null:x.get(0);}
    private void upsertExchange(String key,String type,String direction,String remote,String localId,String remoteId,String localNo,String remoteNo,String status,String payload){jdbc.update("INSERT INTO trading_document_exchange(id,exchange_key,document_type,direction,remote_tenant_id,local_document_id,remote_document_id,local_document_no,remote_document_no,status,payload_json) VALUES(?,?,?,?,?,?,?,?,?,?,CAST(? AS JSON)) ON DUPLICATE KEY UPDATE remote_document_id=VALUES(remote_document_id),remote_document_no=VALUES(remote_document_no),status=VALUES(status),payload_json=VALUES(payload_json)",UUID.randomUUID().toString(),key,type,direction,remote,localId,remoteId,localNo,remoteNo,status,payload);}
    private Map<String,Object> getExchange(String key){return one("SELECT * FROM trading_document_exchange WHERE exchange_key=?",key);}
    private Map<String,Object> one(String sql,Object...args){List<Map<String,Object>>x=jdbc.queryForList(sql,args);if(x.isEmpty())throw new IllegalArgumentException("Required record was not found or is not authorised");return x.get(0);}
    private String table(String tenant,String name){return "`"+tenant(tenant)+"`.`"+name+"`";}
    private String tenant(String value){if(!StringUtils.hasText(value)||!TENANT.matcher(value.trim()).matches())throw new IllegalArgumentException("Invalid tenant identifier");return value.trim();}
    private String required(String v,String n){if(!StringUtils.hasText(v))throw new IllegalArgumentException(n+" is required");return v.trim();}
    private String text(Object v){return v==null?"":String.valueOf(v);}
    private String blank(String v){return StringUtils.hasText(v)?v.trim():null;}
    private String defaultText(String v,String d){return StringUtils.hasText(v)?v.trim():d;}
    private String actor(String v){return StringUtils.hasText(v)?v.trim():"SYSTEM";}
    private BigDecimal decimal(Object v){return v==null?BigDecimal.ZERO:new BigDecimal(String.valueOf(v));}
    private LocalDateTime timestamp(Object v){if(v==null)return null;if(v instanceof Timestamp t)return t.toLocalDateTime();if(v instanceof LocalDateTime d)return d;if(v instanceof java.sql.Date d)return d.toLocalDate().atStartOfDay();return LocalDateTime.parse(String.valueOf(v).replace(' ','T'));}
    private String toJson(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("Unable to serialize trading document",e);}}

    private void requireEnabled(){if(!"ENABLED".equalsIgnoreCase(settings.getSetting("STATUS","MAWA-SUPPLIER-NETWORK")))throw new IllegalStateException("MAWA Supplier Network is disabled for this tenant");}

    public record ConfigurationRequest(Boolean enabled){}
    public record ConnectionRequest(String id,String remoteTenantId,String localPartnerId,String remotePartnerId,String relationshipType,String status,Boolean allowPurchaseOrders,Boolean allowInvoices,LocalDate effectiveFrom,LocalDate effectiveTo){}
    public record LineResponse(String id,BigDecimal confirmedQuantity,BigDecimal unitPrice,String status,String note){}
    public record ResponseRequest(String status,String note,LocalDateTime scheduledStartAt,LocalDateTime scheduledEndAt,List<LineResponse> lines){}
    public record ReservationRequest(String lineId,String assetId,String employeePartnerId,BigDecimal quantity,LocalDateTime startAt,LocalDateTime endAt,String notes){}
}
