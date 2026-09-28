package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.stock.InventoryDtos;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.service.NumberRangeService;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InventoryManagementService {
    private final JdbcTemplate jdbcTemplate;
    private final NumberRangeService numberRangeService;
    private final InventoryAccessService access;
    private final InventoryPostingService postingService;
    private final ApprovalService approvalService;

    public InventoryManagementService(JdbcTemplate jdbcTemplate, NumberRangeService numberRangeService, InventoryAccessService access, InventoryPostingService postingService, ApprovalService approvalService) {
        this.jdbcTemplate = jdbcTemplate;
        this.numberRangeService = numberRangeService;
        this.access = access;
        this.postingService = postingService;
        this.approvalService = approvalService;
    }

    @Transactional
    public Map<String,Object> createDocument(InventoryDtos.InventoryDocumentRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) throw new IllegalArgumentException("At least one document line is required");
        String type = required(request.getDocumentType(), "documentType").trim().toUpperCase(Locale.ROOT);
        requireWorkcentreForType(type);
        access.requireWarehouse(request.getWarehouseId());
        access.requireWarehouse(request.getDestinationWarehouseId());
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            List<Map<String,Object>> existing = jdbcTemplate.queryForList("SELECT * FROM inventory_document WHERE idempotency_key=?", request.getIdempotencyKey());
            if (!existing.isEmpty()) return document(text(existing.get(0).get("id")));
        }
        String actor = access.actorId();
        String id = uuid();
        String no = nextNumber("INVENTORY_DOCUMENT", type.substring(0, Math.min(type.length(),3)));
        jdbcTemplate.update("INSERT INTO inventory_document(id,document_no,document_type,status,warehouse_id,destination_warehouse_id,reference_type,reference_id,reference_no,reason_code,requested_date,posting_date,notes,idempotency_key,created_at,created_by,updated_at,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,no,type,"DRAFT",empty(request.getWarehouseId()),empty(request.getDestinationWarehouseId()),empty(request.getReferenceType()),empty(request.getReferenceId()),empty(request.getReferenceNo()),empty(request.getReasonCode()),
                request.getRequestedDate()==null?null:Date.valueOf(request.getRequestedDate()),request.getPostingDate()==null?null:Date.valueOf(request.getPostingDate()),request.getNotes(),empty(request.getIdempotencyKey()),now(),actor,now(),actor);
        int lineNo=10;
        for (InventoryDtos.InventoryDocumentLineRequest line: request.getLines()) {
            if (line.getQuantity()==null || line.getQuantity().compareTo(BigDecimal.ZERO)<=0) throw new IllegalArgumentException("quantity must be greater than zero");
            String lineId=uuid();
            jdbcTemplate.update("INSERT INTO inventory_document_line(id,inventory_document_id,line_no,product_id,quantity,uom,source_location_id,destination_location_id,source_stock_status,destination_stock_status,batch_no,expiry_date,unit_cost,notes,created_at,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    lineId,id,lineNo,required(line.getProductId(),"productId"),line.getQuantity(),defaultText(line.getUom(),"EA"),empty(line.getSourceLocationId()),empty(line.getDestinationLocationId()),empty(line.getSourceStockStatus()),empty(line.getDestinationStockStatus()),empty(line.getBatchNo()),line.getExpiryDate()==null?null:Date.valueOf(line.getExpiryDate()),line.getUnitCost(),line.getNotes(),now(),actor);
            if(line.getSerialNumbers()!=null){
                for(String serial:line.getSerialNumbers()){
                    if(!StringUtils.hasText(serial))continue;
                    jdbcTemplate.update("INSERT INTO inventory_document_line_serial(id,inventory_document_line_id,serial_no,created_at,created_by) VALUES(?,?,?,?,?)",uuid(),lineId,serial.trim(),now(),actor);
                }
            }
            lineNo+=10;
        }
        return document(id);
    }

    @Transactional
    public Map<String,Object> postDocument(String id) {
        return postDocumentInternal(id, true);
    }

    private Map<String,Object> postDocumentInternal(String id, boolean enforceOperationalWorkcentre) {
        Map<String,Object> doc = document(id);
        if ("POSTED".equalsIgnoreCase(text(doc.get("status")))) return doc;
        String type = text(doc.get("document_type"));
        if (enforceOperationalWorkcentre) requireWorkcentreForType(type);
        access.requireAnyWarehouse(text(doc.get("warehouse_id")), text(doc.get("destination_warehouse_id")));
        if ("WAREHOUSE_TRANSFER".equalsIgnoreCase(type)) throw new IllegalStateException("Warehouse transfers must be dispatched and received through the warehouse transfer lifecycle");
        ApprovalType requiredApproval = approvalTypeForDocument(doc);
        if (requiredApproval != null && !"APPROVED".equalsIgnoreCase(text(doc.get("status")))) {
            throw new IllegalStateException("This inventory document requires approval before posting. Submit it for approval first.");
        }
        @SuppressWarnings("unchecked") List<Map<String,Object>> lines=(List<Map<String,Object>>)doc.get("lines");
        InventoryDtos.PostingRequest posting=new InventoryDtos.PostingRequest();
        posting.setDocumentId(id); posting.setMovementType(type); posting.setReferenceType(text(doc.get("reference_type"))); posting.setReferenceId(text(doc.get("reference_id"))); posting.setReferenceNo(text(doc.get("document_no")));
        if (doc.get("posting_date") != null) posting.setPostingDate(((Date)doc.get("posting_date")).toLocalDate());
        posting.setIdempotencyKey("DOCUMENT:"+id); posting.setNotes(text(doc.get("notes")));
        for (Map<String,Object> row:lines) {
            InventoryDtos.PostingLineRequest line=new InventoryDtos.PostingLineRequest();
            line.setProductId(text(row.get("product_id"))); line.setQuantity(decimal(row.get("quantity"))); line.setUom(text(row.get("uom"))); line.setWarehouseId(text(doc.get("warehouse_id")));
            line.setSourceWarehouseId(text(doc.get("warehouse_id"))); line.setDestinationWarehouseId(StringUtils.hasText(text(doc.get("destination_warehouse_id")))?text(doc.get("destination_warehouse_id")):text(doc.get("warehouse_id")));
            line.setSourceLocationId(text(row.get("source_location_id"))); line.setDestinationLocationId(text(row.get("destination_location_id"))); line.setSourceStockStatus(text(row.get("source_stock_status"))); line.setDestinationStockStatus(text(row.get("destination_stock_status"))); line.setBatchNo(text(row.get("batch_no"))); if(row.get("expiry_date")!=null) line.setExpiryDate(((Date)row.get("expiry_date")).toLocalDate()); line.setUnitCost(row.get("unit_cost")==null?null:decimal(row.get("unit_cost")));
            Object serials=row.get("serial_numbers"); if(serials instanceof List<?> list) line.setSerialNumbers(list.stream().map(Object::toString).toList());
            posting.getLines().add(line);
        }
        Map<String,Object> postingResult = postingService.post(posting);
        if ("MOVEMENT_REVERSAL".equalsIgnoreCase(type) && StringUtils.hasText(text(doc.get("reference_id")))) {
            jdbcTemplate.update("UPDATE stock_movement SET reversal_of_movement_id=? WHERE posting_id=?", text(doc.get("reference_id")), postingResult.get("id"));
        }
        Map<String,Object> posted = document(id);
        refreshLinkedStockCount(posted);
        return posted;
    }

    public List<Map<String,Object>> documents(String type, String status, String warehouseId) {
        if (StringUtils.hasText(type)) requireWorkcentreForType(type); else access.requireWorkcentre("stock-movement");
        access.requireWarehouse(warehouseId);
        StringBuilder sql=new StringBuilder("SELECT * FROM inventory_document WHERE 1=1"); List<Object> args=new ArrayList<>();
        if(StringUtils.hasText(type)){sql.append(" AND document_type=?");args.add(type.trim().toUpperCase());}
        if(StringUtils.hasText(status)){sql.append(" AND status=?");args.add(status.trim().toUpperCase());}
        if(StringUtils.hasText(warehouseId)){sql.append(" AND (warehouse_id=? OR destination_warehouse_id=?)");args.add(warehouseId);args.add(warehouseId);}
        else appendDualWarehouseScope(sql,args,"warehouse_id","destination_warehouse_id");
        sql.append(" ORDER BY created_at DESC LIMIT 500"); return jdbcTemplate.queryForList(sql.toString(),args.toArray());
    }

    public Map<String,Object> document(String id) {
        Map<String,Object> d=jdbcTemplate.queryForMap("SELECT * FROM inventory_document WHERE id=?",id);
        List<Map<String,Object>> lines=jdbcTemplate.queryForList("SELECT l.*,p.code product_code,p.description product_description FROM inventory_document_line l LEFT JOIN product p ON p.id=l.product_id WHERE inventory_document_id=? ORDER BY line_no",id);
        for(Map<String,Object> line:lines){
            line.put("serial_numbers",jdbcTemplate.queryForList("SELECT serial_no FROM inventory_document_line_serial WHERE inventory_document_line_id=? ORDER BY serial_no",line.get("id")).stream().map(r->text(r.get("serial_no"))).toList());
        }
        d.put("lines",lines);
        return d;
    }

    public Map<String,Object> authorizedDocument(String id) {
        Map<String,Object> d = document(id);
        requireWorkcentreForType(text(d.get("document_type")));
        access.requireAnyWarehouse(text(d.get("warehouse_id")),text(d.get("destination_warehouse_id")));
        return d;
    }

    public List<Map<String,Object>> referenceWarehouses(String workcentre) {
        requireInventoryReferenceWorkcentre(workcentre);
        StringBuilder sql=new StringBuilder("SELECT id,warehouse_code,name,status FROM warehouse WHERE UPPER(COALESCE(status,'ACTIVE'))='ACTIVE'");List<Object>a=new ArrayList<>();appendWarehouseScope(sql,a,"id");sql.append(" ORDER BY warehouse_code");return jdbcTemplate.queryForList(sql.toString(),a.toArray());
    }

    public List<Map<String,Object>> referenceLocations(String workcentre, String warehouseId) {
        requireInventoryReferenceWorkcentre(workcentre);
        access.requireWarehouse(warehouseId);
        return jdbcTemplate.queryForList("SELECT l.id,l.warehouse_id,l.location_code,l.name,l.location_type,t.available_for_issue,t.allow_putaway,t.allow_picking,t.allow_reservation,t.requires_quality_release FROM storage_location l LEFT JOIN storage_location_type t ON t.code=l.location_type WHERE l.warehouse_id=? AND UPPER(COALESCE(l.status,'ACTIVE'))='ACTIVE' ORDER BY l.location_code", warehouseId);
    }

    public List<Map<String,Object>> referenceProducts(String workcentre, String query) {
        requireInventoryReferenceWorkcentre(workcentre);
        String q = StringUtils.hasText(query) ? "%" + query.trim() + "%" : "%";
        return jdbcTemplate.queryForList("SELECT DISTINCT p.id,p.code,p.description,p.type,COALESCE(NULLIF(TRIM(p.uom),''),'EA') uom FROM product p LEFT JOIN product_barcode b ON b.product_id=p.id WHERE (p.code LIKE ? OR p.description LIKE ? OR b.barcode LIKE ?) AND (p.valid_from IS NULL OR p.valid_from<=CURRENT_DATE) AND (p.valid_to IS NULL OR p.valid_to>=CURRENT_DATE) ORDER BY p.code LIMIT 200", q, q, q);
    }

    private void requireInventoryReferenceWorkcentre(String workcentre) {
        String wc = required(workcentre, "workcentre").trim().toLowerCase(Locale.ROOT);
        Set<String> allowed = Set.of("inventory-dashboard","stock-on-hand","goods-receipt","quality-inspection","putaway","stock-replenishment","stock-transfer","warehouse-transfer","stock-reservation","stock-picking","stock-dispatch","stock-returns","stock-count","stock-adjustment","stock-writeoff","stock-reversal","stock-movement","inventory-audit","inventory-setup");
        if (!allowed.contains(wc)) throw new IllegalArgumentException("Unknown inventory workcentre");
        access.requireWorkcentre(wc);
    }

    public List<Map<String,Object>> availability(String productId,String warehouseId) {
        access.requireWorkcentre("stock-on-hand"); access.requireWarehouse(warehouseId);
        StringBuilder sql=new StringBuilder("SELECT b.product_id,p.code product_code,p.description product_description,b.warehouse_id,w.warehouse_code,b.stock_status,SUM(b.on_hand_qty) on_hand_qty,SUM(b.reserved_qty) reserved_qty,SUM(b.available_qty) available_qty,SUM(b.inventory_value) inventory_value FROM stock_balance b LEFT JOIN product p ON p.id=b.product_id LEFT JOIN warehouse w ON w.id=b.warehouse_id WHERE 1=1"); List<Object>a=new ArrayList<>();
        if(StringUtils.hasText(productId)){sql.append(" AND b.product_id=?");a.add(productId);} if(StringUtils.hasText(warehouseId)){sql.append(" AND b.warehouse_id=?");a.add(warehouseId);} else appendWarehouseScope(sql,a,"b.warehouse_id"); sql.append(" GROUP BY b.product_id,p.code,p.description,b.warehouse_id,w.warehouse_code,b.stock_status ORDER BY p.code,w.warehouse_code,b.stock_status");
        return jdbcTemplate.queryForList(sql.toString(),a.toArray());
    }

    @Transactional
    public Map<String,Object> submitForApproval(String id) {
        Map<String,Object> doc=authorizedDocument(id);
        if("POSTED".equalsIgnoreCase(text(doc.get("status")))) throw new IllegalStateException("Posted inventory documents cannot be submitted for approval");
        ApprovalType type=approvalTypeForDocument(doc);
        if(type==null) return postDocument(id);
        String existing=text(doc.get("approval_request_id"));
        if(StringUtils.hasText(existing) && "PENDING_APPROVAL".equalsIgnoreCase(text(doc.get("status")))) return doc;
        String actor=access.actorId();
        ApprovalSubmitRequest req=new ApprovalSubmitRequest();
        req.setApprovalType(type);req.setReferenceId(id);req.setReferenceNo(text(doc.get("document_no")));
        req.setTitle(humanize(text(doc.get("document_type")))+" "+text(doc.get("document_no")));
        req.setDescription("Inventory document "+text(doc.get("document_no"))+" requires approval before stock can be posted.");
        req.setRequesterId(actor);req.setPayloadJson("{\"documentId\":\""+id+"\",\"documentNo\":\""+text(doc.get("document_no"))+"\",\"documentType\":\""+text(doc.get("document_type"))+"\"}");
        ApprovalRequestResponse response=approvalService.submitForApproval(req);
        jdbcTemplate.update("UPDATE inventory_document SET approval_request_id=?,updated_at=?,updated_by=? WHERE id=?",response.getId(),now(),actor,id);
        return authorizedDocument(id);
    }

    @Transactional
    public void markApprovalSubmitted(String id,String approvalRequestId,String actor){
        jdbcTemplate.update("UPDATE inventory_document SET status='PENDING_APPROVAL',approval_request_id=?,submitted_at=?,submitted_by=?,updated_at=?,updated_by=? WHERE id=?",approvalRequestId,now(),actor,now(),actor,id);
    }

    @Transactional
    public void completeApproval(String id,boolean approved,String actor,String reason){
        Map<String,Object> doc=document(id);
        if(approved){
            jdbcTemplate.update("UPDATE inventory_document SET status='APPROVED',approved_at=?,approved_by=?,updated_at=?,updated_by=? WHERE id=?",now(),actor,now(),actor,id);
            if (!"WAREHOUSE_TRANSFER".equalsIgnoreCase(text(doc.get("document_type")))) postDocumentInternal(id, false);
        }else{
            jdbcTemplate.update("UPDATE inventory_document SET status='DRAFT',approval_request_id=NULL,updated_at=?,updated_by=?,notes=CONCAT(COALESCE(notes,''),?) WHERE id=?",now(),actor,reason==null?"":"\nApproval rejected: "+reason,id);
        }
    }

    private ApprovalType approvalTypeForDocument(Map<String,Object> doc){
        String type=defaultText(text(doc.get("document_type")),"").toUpperCase(Locale.ROOT);
        ApprovalType mapped=switch(type){
            case "ADJUSTMENT_IN","ADJUSTMENT_OUT" -> ApprovalType.INVENTORY_ADJUSTMENT;
            case "STOCKTAKE_GAIN","STOCKTAKE_LOSS" -> ApprovalType.STOCKTAKE_VARIANCE;
            case "DAMAGE_TRANSFER","EXPIRY_TRANSFER","SCRAP_TRANSFER","DISPOSAL_ISSUE" -> ApprovalType.STOCK_WRITE_OFF;
            case "MOVEMENT_REVERSAL" -> ApprovalType.STOCK_REVERSAL;
            case "TRANSFER_OUT","WAREHOUSE_TRANSFER" -> ApprovalType.WAREHOUSE_TRANSFER;
            case "RETURN_TO_SUPPLIER" -> ApprovalType.SUPPLIER_RETURN;
            default -> null;
        };
        if(mapped!=null)return mapped;
        String reason=text(doc.get("reason_code"));
        if(StringUtils.hasText(reason)){
            Integer required=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_reason_code WHERE code=? AND requires_approval=1",Integer.class,reason);
            if(required!=null&&required>0)return ApprovalType.INVENTORY_ADJUSTMENT;
        }
        return null;
    }

    private String humanize(String value){return defaultText(value,"").toLowerCase(Locale.ROOT).replace('_',' ');}

    @Transactional
    public Map<String,Object> createWarehouseTransfer(InventoryDtos.InventoryDocumentRequest request) {
        access.requireWorkcentre("warehouse-transfer");
        if (request == null) throw new IllegalArgumentException("Transfer request is required");
        request.setDocumentType("WAREHOUSE_TRANSFER");
        if (!StringUtils.hasText(request.getWarehouseId()) || !StringUtils.hasText(request.getDestinationWarehouseId())) throw new IllegalArgumentException("Source and destination warehouses are required");
        if (request.getWarehouseId().equals(request.getDestinationWarehouseId())) throw new IllegalArgumentException("Source and destination warehouses must be different for a warehouse transfer");
        return createDocument(request);
    }

    @Transactional
    public Map<String,Object> dispatchWarehouseTransfer(String id) {
        access.requireWorkcentre("warehouse-transfer");
        Map<String,Object> doc = document(id);
        if (!"WAREHOUSE_TRANSFER".equalsIgnoreCase(text(doc.get("document_type")))) throw new IllegalArgumentException("Document is not a warehouse transfer");
        access.requireWarehouse(text(doc.get("warehouse_id")));
        access.requireWarehouse(text(doc.get("destination_warehouse_id")));
        String status = text(doc.get("status"));
        if ("IN_TRANSIT".equalsIgnoreCase(status) || "PARTIALLY_RECEIVED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "COMPLETED_WITH_VARIANCE".equalsIgnoreCase(status)) return doc;
        if (!"APPROVED".equalsIgnoreCase(status)) throw new IllegalStateException("Warehouse transfer must be approved before dispatch");
        @SuppressWarnings("unchecked") List<Map<String,Object>> lines=(List<Map<String,Object>>)doc.get("lines");
        InventoryDtos.PostingRequest posting = new InventoryDtos.PostingRequest();
        posting.setMovementType("TRANSFER_OUT"); posting.setReferenceType("WAREHOUSE_TRANSFER"); posting.setReferenceId(id); posting.setReferenceNo(text(doc.get("document_no")));
        posting.setIdempotencyKey("WAREHOUSE_TRANSFER:DISPATCH:"+id); posting.setNotes(text(doc.get("notes")));
        for (Map<String,Object> row: lines) {
            InventoryDtos.PostingLineRequest line = new InventoryDtos.PostingLineRequest();
            line.setProductId(text(row.get("product_id"))); line.setQuantity(decimal(row.get("quantity"))); line.setUom(defaultText(text(row.get("uom")),"EA"));
            line.setSourceWarehouseId(text(doc.get("warehouse_id"))); line.setSourceLocationId(text(row.get("source_location_id"))); line.setSourceStockStatus(defaultText(text(row.get("source_stock_status")),"UNRESTRICTED"));
            line.setBatchNo(text(row.get("batch_no"))); line.setUnitCost(row.get("unit_cost")==null?null:decimal(row.get("unit_cost")));
            Object serials=row.get("serial_numbers"); if(serials instanceof List<?> list) line.setSerialNumbers(list.stream().map(Object::toString).toList());
            posting.getLines().add(line);
        }
        postingService.post(posting);
        String actor=access.actorId();
        jdbcTemplate.update("UPDATE inventory_document_line SET dispatched_qty=quantity WHERE inventory_document_id=?",id);
        jdbcTemplate.update("UPDATE inventory_document SET status='IN_TRANSIT',posted_at=?,posted_by=?,updated_at=?,updated_by=? WHERE id=?",now(),actor,now(),actor,id);
        return document(id);
    }

    @Transactional
    public Map<String,Object> receiveWarehouseTransfer(String id, InventoryDtos.WarehouseTransferReceiptRequest request) {
        access.requireWorkcentre("warehouse-transfer");
        Map<String,Object> doc = document(id);
        if (!"WAREHOUSE_TRANSFER".equalsIgnoreCase(text(doc.get("document_type")))) throw new IllegalArgumentException("Document is not a warehouse transfer");
        if (!("IN_TRANSIT".equalsIgnoreCase(text(doc.get("status"))) || "PARTIALLY_RECEIVED".equalsIgnoreCase(text(doc.get("status"))))) throw new IllegalStateException("Only dispatched warehouse transfers can be received");
        String destinationWarehouse = required(text(doc.get("destination_warehouse_id")),"destinationWarehouseId");
        access.requireWarehouse(destinationWarehouse);
        Map<String,Map<String,Object>> existingLines = new HashMap<>();
        @SuppressWarnings("unchecked") List<Map<String,Object>> docLines=(List<Map<String,Object>>)doc.get("lines");
        for(Map<String,Object> row:docLines) existingLines.put(text(row.get("id")),row);
        List<InventoryDtos.WarehouseTransferReceiptLineRequest> receiptLines = request==null?List.of():request.getLines();
        if(receiptLines==null||receiptLines.isEmpty()) throw new IllegalArgumentException("At least one received transfer line is required");
        InventoryDtos.PostingRequest posting = new InventoryDtos.PostingRequest();
        posting.setMovementType("TRANSFER_IN"); posting.setReferenceType("WAREHOUSE_TRANSFER"); posting.setReferenceId(id); posting.setReferenceNo(text(doc.get("document_no")));
        posting.setIdempotencyKey("WAREHOUSE_TRANSFER:RECEIPT:"+id+":"+System.currentTimeMillis()); posting.setNotes(request.getNotes());
        String actor=access.actorId();
        for(InventoryDtos.WarehouseTransferReceiptLineRequest receipt:receiptLines){
            Map<String,Object> row=existingLines.get(receipt.getLineId()); if(row==null) throw new IllegalArgumentException("Transfer line not found: "+receipt.getLineId());
            BigDecimal accepted=receipt.getReceivedQty()==null?BigDecimal.ZERO:receipt.getReceivedQty(); BigDecimal damaged=receipt.getDamagedQty()==null?BigDecimal.ZERO:receipt.getDamagedQty();
            if(accepted.compareTo(BigDecimal.ZERO)<0||damaged.compareTo(BigDecimal.ZERO)<0) throw new IllegalArgumentException("Received and damaged quantities cannot be negative");
            BigDecimal outstanding=decimal(row.get("dispatched_qty")).subtract(decimal(row.get("received_qty"))).subtract(decimal(row.get("damaged_qty")));
            if(accepted.add(damaged).compareTo(outstanding)>0) throw new IllegalStateException("Received quantity exceeds outstanding transfer quantity for line "+row.get("line_no"));
            String destinationLocation=required(text(row.get("destination_location_id")),"destinationLocationId");
            if(accepted.compareTo(BigDecimal.ZERO)>0){
                InventoryDtos.PostingLineRequest pl=new InventoryDtos.PostingLineRequest();pl.setProductId(text(row.get("product_id")));pl.setDestinationWarehouseId(destinationWarehouse);pl.setDestinationLocationId(destinationLocation);pl.setDestinationStockStatus(defaultText(text(row.get("destination_stock_status")),"UNRESTRICTED"));pl.setQuantity(accepted);pl.setUom(defaultText(text(row.get("uom")),"EA"));pl.setBatchNo(text(row.get("batch_no")));if(row.get("expiry_date")!=null)pl.setExpiryDate(((Date)row.get("expiry_date")).toLocalDate());pl.setUnitCost(row.get("unit_cost")==null?null:decimal(row.get("unit_cost")));pl.setSerialNumbers(receipt.getSerialNumbers());posting.getLines().add(pl);
            }
            if(damaged.compareTo(BigDecimal.ZERO)>0){
                InventoryDtos.PostingLineRequest pl=new InventoryDtos.PostingLineRequest();pl.setProductId(text(row.get("product_id")));pl.setDestinationWarehouseId(destinationWarehouse);pl.setDestinationLocationId(destinationLocation);pl.setDestinationStockStatus("DAMAGED");pl.setQuantity(damaged);pl.setUom(defaultText(text(row.get("uom")),"EA"));pl.setBatchNo(text(row.get("batch_no")));if(row.get("expiry_date")!=null)pl.setExpiryDate(((Date)row.get("expiry_date")).toLocalDate());pl.setUnitCost(row.get("unit_cost")==null?null:decimal(row.get("unit_cost")));pl.setSerialNumbers(receipt.getDamagedSerialNumbers());posting.getLines().add(pl);
            }
            jdbcTemplate.update("UPDATE inventory_document_line SET received_qty=received_qty+?,damaged_qty=damaged_qty+? WHERE id=?",accepted,damaged,receipt.getLineId());
        }
        if(posting.getLines().isEmpty()) throw new IllegalArgumentException("At least one positive received or damaged quantity is required");
        postingService.post(posting);
        boolean finalReceipt=request.getFinalReceipt()!=null&&request.getFinalReceipt();
        BigDecimal outstanding=jdbcTemplate.queryForObject("SELECT COALESCE(SUM(GREATEST(dispatched_qty-received_qty-damaged_qty,0)),0) FROM inventory_document_line WHERE inventory_document_id=?",BigDecimal.class,id);
        if(finalReceipt && outstanding!=null && outstanding.compareTo(BigDecimal.ZERO)>0){
            jdbcTemplate.update("UPDATE inventory_document_line SET shortage_qty=GREATEST(dispatched_qty-received_qty-damaged_qty,0) WHERE inventory_document_id=?",id);
        }
        BigDecimal shortages=jdbcTemplate.queryForObject("SELECT COALESCE(SUM(shortage_qty),0) FROM inventory_document_line WHERE inventory_document_id=?",BigDecimal.class,id);
        BigDecimal damages=jdbcTemplate.queryForObject("SELECT COALESCE(SUM(damaged_qty),0) FROM inventory_document_line WHERE inventory_document_id=?",BigDecimal.class,id);
        String next=(finalReceipt || outstanding==null || outstanding.compareTo(BigDecimal.ZERO)==0) ? ((shortages!=null&&shortages.compareTo(BigDecimal.ZERO)>0)||(damages!=null&&damages.compareTo(BigDecimal.ZERO)>0)?"COMPLETED_WITH_VARIANCE":"COMPLETED") : "PARTIALLY_RECEIVED";
        jdbcTemplate.update("UPDATE inventory_document SET status=?,updated_at=?,updated_by=? WHERE id=?",next,now(),actor,id);
        return document(id);
    }

    @Transactional
    public Map<String,Object> createStockCount(InventoryDtos.StockCountCreateRequest request){
        access.requireWorkcentre("stock-count");
        String warehouseId=required(request.getWarehouseId(),"warehouseId");access.requireWarehouse(warehouseId);
        String actor=access.actorId();String id=uuid();String no=nextNumber("INV_STOCK_COUNT","CNT");
        boolean blind=request.getBlindCount()==null||request.getBlindCount();boolean freeze=request.getFreezeStock()==null||request.getFreezeStock();
        jdbcTemplate.update("INSERT INTO inventory_stock_count(id,count_no,warehouse_id,storage_location_id,count_type,blind_count,freeze_stock,status,snapshot_at,notes,created_at,created_by,updated_at,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,no,warehouseId,empty(request.getStorageLocationId()),defaultText(request.getCountType(),"CYCLE").toUpperCase(Locale.ROOT),blind,freeze,"COUNTING",now(),request.getNotes(),now(),actor,now(),actor);
        StringBuilder sql=new StringBuilder("SELECT product_id,storage_location_id,batch_no,stock_status,on_hand_qty,uom FROM stock_balance WHERE warehouse_id=?");List<Object>a=new ArrayList<>();a.add(warehouseId);
        if(StringUtils.hasText(request.getStorageLocationId())){sql.append(" AND storage_location_id=?");a.add(request.getStorageLocationId());}
        sql.append(" ORDER BY storage_location_id,product_id,batch_no,stock_status");
        for(Map<String,Object> row:jdbcTemplate.queryForList(sql.toString(),a.toArray())){
            jdbcTemplate.update("INSERT INTO inventory_stock_count_line(id,stock_count_id,product_id,storage_location_id,batch_no,stock_status,system_qty,counted_qty,variance_qty,uom) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    uuid(),id,row.get("product_id"),row.get("storage_location_id"),defaultText(text(row.get("batch_no")),""),defaultText(text(row.get("stock_status")),"UNRESTRICTED"),row.get("on_hand_qty"),null,null,defaultText(text(row.get("uom")),"EA"));
        }
        return stockCount(id);
    }

    public List<Map<String,Object>> stockCounts(String status,String warehouseId){
        access.requireWorkcentre("stock-count");access.requireWarehouse(warehouseId);StringBuilder sql=new StringBuilder("SELECT c.*,w.warehouse_code,l.location_code FROM inventory_stock_count c LEFT JOIN warehouse w ON w.id=c.warehouse_id LEFT JOIN storage_location l ON l.id=c.storage_location_id WHERE 1=1");List<Object>a=new ArrayList<>();
        if(StringUtils.hasText(status)){sql.append(" AND c.status=?");a.add(status.toUpperCase(Locale.ROOT));}if(StringUtils.hasText(warehouseId)){sql.append(" AND c.warehouse_id=?");a.add(warehouseId);}else appendWarehouseScope(sql,a,"c.warehouse_id");sql.append(" ORDER BY c.created_at DESC LIMIT 300");return jdbcTemplate.queryForList(sql.toString(),a.toArray());
    }

    public Map<String,Object> stockCount(String id){
        access.requireWorkcentre("stock-count");Map<String,Object> count=jdbcTemplate.queryForMap("SELECT c.*,w.warehouse_code,l.location_code FROM inventory_stock_count c LEFT JOIN warehouse w ON w.id=c.warehouse_id LEFT JOIN storage_location l ON l.id=c.storage_location_id WHERE c.id=?",id);access.requireWarehouse(text(count.get("warehouse_id")));
        count.put("lines",jdbcTemplate.queryForList("SELECT cl.*,p.code product_code,p.description product_description,l.location_code FROM inventory_stock_count_line cl LEFT JOIN product p ON p.id=cl.product_id LEFT JOIN storage_location l ON l.id=cl.storage_location_id WHERE cl.stock_count_id=? ORDER BY l.location_code,p.code,cl.batch_no",id));return count;
    }

    @Transactional
    public Map<String,Object> recordStockCount(String id,InventoryDtos.StockCountUpdateRequest request){
        Map<String,Object> count=stockCount(id);if(!"COUNTING".equalsIgnoreCase(text(count.get("status"))))throw new IllegalStateException("Only counting stock counts can be updated");String actor=access.actorId();
        for(InventoryDtos.StockCountEntryRequest line:request.getLines()){
            if(line.getCountedQty()==null||line.getCountedQty().compareTo(BigDecimal.ZERO)<0)throw new IllegalArgumentException("countedQty cannot be negative");
            int updated=jdbcTemplate.update("UPDATE inventory_stock_count_line SET counted_qty=?,variance_qty=?-system_qty,counted_at=?,counted_by=?,notes=? WHERE id=? AND stock_count_id=?",line.getCountedQty(),line.getCountedQty(),now(),actor,line.getNotes(),line.getLineId(),id);if(updated!=1)throw new IllegalArgumentException("Stock count line not found: "+line.getLineId());
        }
        jdbcTemplate.update("UPDATE inventory_stock_count SET updated_at=?,updated_by=? WHERE id=?",now(),actor,id);return stockCount(id);
    }

    @Transactional
    public Map<String,Object> finalizeStockCount(String id){
        Map<String,Object> count=stockCount(id);if(!"COUNTING".equalsIgnoreCase(text(count.get("status"))))throw new IllegalStateException("Only counting stock counts can be finalised");
        Integer missing=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_stock_count_line WHERE stock_count_id=? AND counted_qty IS NULL",Integer.class,id);if(missing!=null&&missing>0)throw new IllegalStateException("All stock count lines must be counted before finalisation");
        @SuppressWarnings("unchecked") List<Map<String,Object>> lines=(List<Map<String,Object>>)count.get("lines");
        List<InventoryDtos.InventoryDocumentLineRequest> gains=new ArrayList<>(),losses=new ArrayList<>();
        for(Map<String,Object> row:lines){BigDecimal variance=decimal(row.get("variance_qty"));if(variance.compareTo(BigDecimal.ZERO)==0)continue;InventoryDtos.InventoryDocumentLineRequest l=new InventoryDtos.InventoryDocumentLineRequest();l.setProductId(text(row.get("product_id")));l.setQuantity(variance.abs());l.setUom(text(row.get("uom")));l.setBatchNo(text(row.get("batch_no")));if(variance.signum()>0){l.setDestinationLocationId(text(row.get("storage_location_id")));l.setDestinationStockStatus(text(row.get("stock_status")));gains.add(l);}else{l.setSourceLocationId(text(row.get("storage_location_id")));l.setSourceStockStatus(text(row.get("stock_status")));losses.add(l);}}
        if(gains.isEmpty()&&losses.isEmpty()){jdbcTemplate.update("UPDATE inventory_stock_count SET status='COMPLETED',updated_at=?,updated_by=? WHERE id=?",now(),access.actorId(),id);return stockCount(id);}
        if(!gains.isEmpty())submitCountVarianceDocument(count,"STOCKTAKE_GAIN",gains);
        if(!losses.isEmpty())submitCountVarianceDocument(count,"STOCKTAKE_LOSS",losses);
        jdbcTemplate.update("UPDATE inventory_stock_count SET status='PENDING_APPROVAL',updated_at=?,updated_by=? WHERE id=?",now(),access.actorId(),id);refreshLinkedStockCount(count);return stockCount(id);
    }

    private void submitCountVarianceDocument(Map<String,Object> count,String type,List<InventoryDtos.InventoryDocumentLineRequest> lines){
        InventoryDtos.InventoryDocumentRequest request=new InventoryDtos.InventoryDocumentRequest();request.setDocumentType(type);request.setWarehouseId(text(count.get("warehouse_id")));request.setReferenceType("STOCK_COUNT");request.setReferenceId(text(count.get("id")));request.setReferenceNo(text(count.get("count_no")));request.setReasonCode("COUNT_VARIANCE");request.setNotes("Stock count variance from "+text(count.get("count_no")));request.setIdempotencyKey("STOCK_COUNT:"+text(count.get("id"))+":"+type);request.setLines(lines);Map<String,Object> doc=createDocument(request);submitForApproval(text(doc.get("id")));
    }

    private void refreshLinkedStockCount(Map<String,Object> document){
        if(!"STOCK_COUNT".equalsIgnoreCase(text(document.get("reference_type")))||!StringUtils.hasText(text(document.get("reference_id"))))return;String countId=text(document.get("reference_id"));Integer pending=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_document WHERE reference_type='STOCK_COUNT' AND reference_id=? AND status<>'POSTED'",Integer.class,countId);if(pending!=null&&pending==0)jdbcTemplate.update("UPDATE inventory_stock_count SET status='COMPLETED',updated_at=?,updated_by=? WHERE id=?",now(),access.actorId(),countId);
    }

    @Transactional
    public Map<String,Object> createReservation(InventoryDtos.ReservationRequest request) {
        access.requireWorkcentre("stock-reservation");
        access.requireWarehouse(request.getWarehouseId());
        String actor=access.actorId();
        String productId=required(request.getProductId(),"productId");
        BigDecimal requestedQty=request.getQuantity();
        if(requestedQty==null||requestedQty.compareTo(BigDecimal.ZERO)<=0) throw new IllegalArgumentException("quantity must be greater than zero");
        BaseQuantity converted=toBaseQuantity(productId,requestedQty,defaultText(request.getUom(),"EA"));
        BigDecimal qty=converted.quantity();
        String reservationUom=converted.uom();
        String status=defaultText(request.getStockStatus(),"UNRESTRICTED").toUpperCase(Locale.ROOT);
        StringBuilder sql=new StringBuilder("SELECT b.id,b.storage_location_id,b.batch_no,b.available_qty FROM stock_balance b JOIN storage_location l ON l.id=b.storage_location_id LEFT JOIN storage_location_type t ON t.code=l.location_type WHERE b.product_id=? AND b.stock_status=? AND b.available_qty>0 AND COALESCE(t.allow_reservation,1)=1");
        List<Object> args=new ArrayList<>();args.add(productId);args.add(status);
        if(StringUtils.hasText(request.getWarehouseId())){sql.append(" AND b.warehouse_id=?");args.add(request.getWarehouseId());}
        if(StringUtils.hasText(request.getStorageLocationId())){sql.append(" AND b.storage_location_id=?");args.add(request.getStorageLocationId());}
        if(StringUtils.hasText(request.getBatchNo())){sql.append(" AND b.batch_no=?");args.add(request.getBatchNo());}
        sql.append(" ORDER BY b.available_qty DESC FOR UPDATE");
        List<Map<String,Object>> rows=jdbcTemplate.queryForList(sql.toString(),args.toArray());
        BigDecimal remaining=qty; String reservationNo=nextNumber("INV_RESERVATION","RSV"); String firstId=null;
        for(Map<String,Object> row:rows){
            if(remaining.compareTo(BigDecimal.ZERO)<=0)break;
            BigDecimal available=decimal(row.get("available_qty")); BigDecimal take=available.min(remaining); String id=uuid(); if(firstId==null)firstId=id;
            jdbcTemplate.update("UPDATE stock_balance SET reserved_qty=reserved_qty+?,available_qty=available_qty-?,updated_at=?,updated_by=? WHERE id=?",take,take,now(),actor,row.get("id"));
            jdbcTemplate.update("INSERT INTO inventory_reservation(id,reservation_no,source_type,source_id,source_line_id,product_id,warehouse_id,storage_location_id,batch_no,stock_status,quantity,fulfilled_qty,uom,priority,status,required_at,created_at,created_by,updated_at,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id,reservationNo,required(request.getSourceType(),"sourceType"),required(request.getSourceId(),"sourceId"),empty(request.getSourceLineId()),productId,request.getWarehouseId(),row.get("storage_location_id"),row.get("batch_no"),status,take,BigDecimal.ZERO,reservationUom,defaultText(request.getPriority(),"NORMAL").toUpperCase(Locale.ROOT),"ACTIVE",empty(request.getRequiredAt()),now(),actor,now(),actor);
            remaining=remaining.subtract(take);
        }
        if(remaining.compareTo(BigDecimal.ZERO)>0) throw new IllegalStateException("Insufficient available stock to reserve requested quantity");
        return jdbcTemplate.queryForMap("SELECT * FROM inventory_reservation WHERE id=?",firstId);
    }

    public List<Map<String,Object>> reservations(String status,String sourceType,String sourceId){
        access.requireWorkcentre("stock-reservation"); StringBuilder sql=new StringBuilder("SELECT r.*,p.code product_code,p.description product_description,w.warehouse_code,l.location_code FROM inventory_reservation r LEFT JOIN product p ON p.id=r.product_id LEFT JOIN warehouse w ON w.id=r.warehouse_id LEFT JOIN storage_location l ON l.id=r.storage_location_id WHERE 1=1");List<Object>a=new ArrayList<>();
        if(StringUtils.hasText(status)){sql.append(" AND r.status=?");a.add(status.toUpperCase(Locale.ROOT));}if(StringUtils.hasText(sourceType)){sql.append(" AND r.source_type=?");a.add(sourceType.toUpperCase(Locale.ROOT));}if(StringUtils.hasText(sourceId)){sql.append(" AND r.source_id=?");a.add(sourceId);}appendWarehouseScope(sql,a,"r.warehouse_id");sql.append(" ORDER BY r.created_at DESC LIMIT 500");return jdbcTemplate.queryForList(sql.toString(),a.toArray());
    }

    @Transactional
    public Map<String,Object> releaseReservation(String id){
        access.requireWorkcentre("stock-reservation");String actor=access.actorId();Map<String,Object> r=jdbcTemplate.queryForMap("SELECT * FROM inventory_reservation WHERE id=? FOR UPDATE",id);access.requireWarehouse(text(r.get("warehouse_id")));if(!"ACTIVE".equalsIgnoreCase(text(r.get("status"))))return r;BigDecimal release=decimal(r.get("quantity")).subtract(decimal(r.get("fulfilled_qty")));
        jdbcTemplate.update("UPDATE stock_balance SET reserved_qty=GREATEST(reserved_qty-?,0),available_qty=available_qty+?,updated_at=?,updated_by=? WHERE product_id=? AND warehouse_id=? AND storage_location_id=? AND batch_no=? AND stock_status=?",release,release,now(),actor,r.get("product_id"),r.get("warehouse_id"),r.get("storage_location_id"),defaultText(text(r.get("batch_no")),""),r.get("stock_status"));
        jdbcTemplate.update("UPDATE inventory_reservation SET status='RELEASED',updated_at=?,updated_by=? WHERE id=?",now(),actor,id);return jdbcTemplate.queryForMap("SELECT * FROM inventory_reservation WHERE id=?",id);
    }

    public List<Map<String,Object>> reversibleMovements(String warehouseId) {
        access.requireWorkcentre("stock-reversal");
        access.requireWarehouse(warehouseId);
        StringBuilder sql = new StringBuilder("SELECT m.*,p.code product_code,p.description product_description,sw.warehouse_code source_warehouse_code,dw.warehouse_code destination_warehouse_code,fl.location_code from_location_code,tl.location_code to_location_code FROM stock_movement m LEFT JOIN product p ON p.id=m.product_id LEFT JOIN warehouse sw ON sw.id=COALESCE(m.source_warehouse_id,m.warehouse_id) LEFT JOIN warehouse dw ON dw.id=COALESCE(m.destination_warehouse_id,m.warehouse_id) LEFT JOIN storage_location fl ON fl.id=m.from_location_id LEFT JOIN storage_location tl ON tl.id=m.to_location_id WHERE m.reversal_of_movement_id IS NULL AND NOT EXISTS (SELECT 1 FROM stock_movement r WHERE r.reversal_of_movement_id=m.id)");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(warehouseId)) { sql.append(" AND (COALESCE(m.source_warehouse_id,m.warehouse_id)=? OR COALESCE(m.destination_warehouse_id,m.warehouse_id)=?)"); args.add(warehouseId); args.add(warehouseId); }
        else appendDualWarehouseScope(sql,args,"COALESCE(m.source_warehouse_id,m.warehouse_id)","COALESCE(m.destination_warehouse_id,m.warehouse_id)");
        sql.append(" ORDER BY m.movement_at DESC LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public Map<String,Object> reverseMovement(String movementId,String notes){
        access.requireWorkcentre("stock-reversal");
        Map<String,Object> m=jdbcTemplate.queryForMap("SELECT * FROM stock_movement WHERE id=?",movementId);
        access.requireAnyWarehouse(defaultText(text(m.get("source_warehouse_id")),text(m.get("warehouse_id"))),defaultText(text(m.get("destination_warehouse_id")),text(m.get("warehouse_id"))));
        Integer reversed=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE reversal_of_movement_id=?",Integer.class,movementId);
        if(reversed!=null&&reversed>0)throw new IllegalStateException("Stock movement has already been reversed");
        List<Map<String,Object>> existing = jdbcTemplate.queryForList("SELECT * FROM inventory_document WHERE document_type='MOVEMENT_REVERSAL' AND reference_type='STOCK_MOVEMENT' AND reference_id=? AND status NOT IN ('CANCELLED','REJECTED') ORDER BY created_at DESC LIMIT 1",movementId);
        if(!existing.isEmpty()) return authorizedDocument(text(existing.get(0).get("id")));

        InventoryDtos.InventoryDocumentRequest request=new InventoryDtos.InventoryDocumentRequest();
        request.setDocumentType("MOVEMENT_REVERSAL");
        request.setWarehouseId(defaultText(text(m.get("destination_warehouse_id")),text(m.get("warehouse_id"))));
        request.setDestinationWarehouseId(defaultText(text(m.get("source_warehouse_id")),text(m.get("warehouse_id"))));
        request.setReferenceType("STOCK_MOVEMENT");request.setReferenceId(movementId);request.setReferenceNo(text(m.get("movement_no")));request.setNotes(notes);request.setIdempotencyKey("REVERSAL-DOCUMENT:"+movementId);
        InventoryDtos.InventoryDocumentLineRequest line=new InventoryDtos.InventoryDocumentLineRequest();
        line.setProductId(text(m.get("product_id")));line.setSourceLocationId(text(m.get("to_location_id")));line.setDestinationLocationId(text(m.get("from_location_id")));line.setSourceStockStatus(text(m.get("stock_status_to")));line.setDestinationStockStatus(text(m.get("stock_status_from")));line.setQuantity(decimal(m.get("quantity")));line.setUom(text(m.get("uom")));line.setBatchNo(text(m.get("batch_no")));line.setUnitCost(decimal(m.get("unit_cost")));request.getLines().add(line);
        Map<String,Object> document=createDocument(request);
        return submitForApproval(text(document.get("id")));
    }

    public List<Map<String,Object>> reasonCodes(){ access.requireWorkcentre("inventory-setup"); return jdbcTemplate.queryForList("SELECT * FROM inventory_reason_code WHERE active=1 ORDER BY category,description"); }

    @Transactional
    public Map<String,Object> saveReasonCode(InventoryDtos.ReasonCodeRequest request){
        access.requireWorkcentre("inventory-setup");String code=required(request.getCode(),"code").toUpperCase(Locale.ROOT);String actor=access.actorId();
        jdbcTemplate.update("INSERT INTO inventory_reason_code(code,description,category,requires_notes,requires_attachment,requires_approval,active,created_at,created_by,updated_at,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE description=VALUES(description),category=VALUES(category),requires_notes=VALUES(requires_notes),requires_attachment=VALUES(requires_attachment),requires_approval=VALUES(requires_approval),active=VALUES(active),updated_at=VALUES(updated_at),updated_by=VALUES(updated_by)",code,required(request.getDescription(),"description"),defaultText(request.getCategory(),"OTHER").toUpperCase(Locale.ROOT),Boolean.TRUE.equals(request.getRequiresNotes()),Boolean.TRUE.equals(request.getRequiresAttachment()),Boolean.TRUE.equals(request.getRequiresApproval()),request.getActive()==null||request.getActive(),now(),actor,now(),actor);
        return jdbcTemplate.queryForMap("SELECT * FROM inventory_reason_code WHERE code=?",code);
    }

    public List<Map<String,Object>> productWarehousePolicies(String productId,String warehouseId) {
        access.requireWorkcentre("inventory-setup");
        StringBuilder sql=new StringBuilder("SELECT pol.*,p.code product_code,p.description product_description,w.warehouse_code,l.location_code preferred_picking_location_code FROM inventory_product_warehouse_policy pol JOIN product p ON p.id=pol.product_id JOIN warehouse w ON w.id=pol.warehouse_id LEFT JOIN storage_location l ON l.id=pol.preferred_picking_location_id WHERE 1=1");
        List<Object> args=new ArrayList<>();
        if(StringUtils.hasText(productId)){sql.append(" AND pol.product_id=?");args.add(productId);}
        if(StringUtils.hasText(warehouseId)){access.requireWarehouse(warehouseId);sql.append(" AND pol.warehouse_id=?");args.add(warehouseId);}else appendWarehouseScope(sql,args,"pol.warehouse_id");
        sql.append(" ORDER BY p.code,w.warehouse_code");return jdbcTemplate.queryForList(sql.toString(),args.toArray());
    }

    @Transactional
    public Map<String,Object> saveProductWarehousePolicy(InventoryDtos.ProductWarehousePolicyRequest request) {
        access.requireWorkcentre("inventory-setup");String productId=required(request.getProductId(),"productId");String warehouseId=required(request.getWarehouseId(),"warehouseId");access.requireWarehouse(warehouseId);String actor=access.actorId();
        if(StringUtils.hasText(request.getPreferredPickingLocationId())){
            Integer ok=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_location WHERE id=? AND warehouse_id=?",Integer.class,request.getPreferredPickingLocationId(),warehouseId);if(ok==null||ok!=1)throw new IllegalArgumentException("Preferred picking location must belong to the selected warehouse");
        }
        jdbcTemplate.update("INSERT INTO inventory_product_warehouse_policy(product_id,warehouse_id,minimum_qty,maximum_qty,reorder_point,reorder_qty,safety_stock_qty,preferred_supplier_partner_id,lead_time_days,preferred_picking_location_id,expiry_warning_days,minimum_shelf_life_days,updated_at,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE minimum_qty=VALUES(minimum_qty),maximum_qty=VALUES(maximum_qty),reorder_point=VALUES(reorder_point),reorder_qty=VALUES(reorder_qty),safety_stock_qty=VALUES(safety_stock_qty),preferred_supplier_partner_id=VALUES(preferred_supplier_partner_id),lead_time_days=VALUES(lead_time_days),preferred_picking_location_id=VALUES(preferred_picking_location_id),expiry_warning_days=VALUES(expiry_warning_days),minimum_shelf_life_days=VALUES(minimum_shelf_life_days),updated_at=VALUES(updated_at),updated_by=VALUES(updated_by)",
                productId,warehouseId,nvl(request.getMinimumQty()),nvl(request.getMaximumQty()),nvl(request.getReorderPoint()),nvl(request.getReorderQty()),nvl(request.getSafetyStockQty()),empty(request.getPreferredSupplierPartnerId()),request.getLeadTimeDays()==null?0:Math.max(request.getLeadTimeDays(),0),empty(request.getPreferredPickingLocationId()),request.getExpiryWarningDays()==null?30:Math.max(request.getExpiryWarningDays(),0),request.getMinimumShelfLifeDays()==null?0:Math.max(request.getMinimumShelfLifeDays(),0),now(),actor);
        return jdbcTemplate.queryForMap("SELECT * FROM inventory_product_warehouse_policy WHERE product_id=? AND warehouse_id=?",productId,warehouseId);
    }

    public List<Map<String,Object>> replenishmentRecommendations(String warehouseId) {
        access.requireWorkcentre("stock-replenishment");access.requireWarehouse(warehouseId);
        StringBuilder sql=new StringBuilder("""
            SELECT pol.product_id,p.code product_code,p.description product_description,pol.warehouse_id,w.warehouse_code,
                   pol.preferred_picking_location_id,pl.location_code preferred_picking_location_code,
                   COALESCE(SUM(CASE WHEN b.stock_status='UNRESTRICTED' THEN b.available_qty ELSE 0 END),0) warehouse_available_qty,
                   COALESCE(SUM(CASE WHEN b.storage_location_id=pol.preferred_picking_location_id AND b.stock_status='UNRESTRICTED' THEN b.available_qty ELSE 0 END),0) picking_available_qty,
                   pol.minimum_qty,pol.maximum_qty,pol.reorder_point,pol.reorder_qty,pol.safety_stock_qty,
                   GREATEST(pol.reorder_qty,pol.maximum_qty-COALESCE(SUM(CASE WHEN b.storage_location_id=pol.preferred_picking_location_id AND b.stock_status='UNRESTRICTED' THEN b.available_qty ELSE 0 END),0)) suggested_qty
              FROM inventory_product_warehouse_policy pol
              JOIN product p ON p.id=pol.product_id JOIN warehouse w ON w.id=pol.warehouse_id
              LEFT JOIN storage_location pl ON pl.id=pol.preferred_picking_location_id
              LEFT JOIN stock_balance b ON b.product_id=pol.product_id AND b.warehouse_id=pol.warehouse_id
             WHERE pol.preferred_picking_location_id IS NOT NULL
            """);List<Object>a=new ArrayList<>();if(StringUtils.hasText(warehouseId)){sql.append(" AND pol.warehouse_id=?");a.add(warehouseId);}else appendWarehouseScope(sql,a,"pol.warehouse_id");sql.append(" GROUP BY pol.product_id,p.code,p.description,pol.warehouse_id,w.warehouse_code,pol.preferred_picking_location_id,pl.location_code,pol.minimum_qty,pol.maximum_qty,pol.reorder_point,pol.reorder_qty,pol.safety_stock_qty HAVING picking_available_qty<=pol.reorder_point AND warehouse_available_qty>picking_available_qty ORDER BY p.code");return jdbcTemplate.queryForList(sql.toString(),a.toArray());
    }

    public List<Map<String,Object>> purchaseRecommendations(String warehouseId) {
        access.requireWorkcentre("stock-on-hand");access.requireWarehouse(warehouseId);
        StringBuilder sql=new StringBuilder("""
            SELECT pol.product_id,p.code product_code,p.description product_description,pol.warehouse_id,w.warehouse_code,
                   pol.preferred_supplier_partner_id,pol.lead_time_days,pol.reorder_point,pol.reorder_qty,pol.safety_stock_qty,
                   COALESCE(SUM(CASE WHEN b.stock_status='UNRESTRICTED' THEN b.available_qty ELSE 0 END),0) available_qty,
                   GREATEST(pol.reorder_qty,pol.safety_stock_qty+pol.reorder_point-COALESCE(SUM(CASE WHEN b.stock_status='UNRESTRICTED' THEN b.available_qty ELSE 0 END),0)) suggested_order_qty
              FROM inventory_product_warehouse_policy pol JOIN product p ON p.id=pol.product_id JOIN warehouse w ON w.id=pol.warehouse_id
              LEFT JOIN stock_balance b ON b.product_id=pol.product_id AND b.warehouse_id=pol.warehouse_id
             WHERE 1=1
            """);List<Object>a=new ArrayList<>();if(StringUtils.hasText(warehouseId)){sql.append(" AND pol.warehouse_id=?");a.add(warehouseId);}else appendWarehouseScope(sql,a,"pol.warehouse_id");sql.append(" GROUP BY pol.product_id,p.code,p.description,pol.warehouse_id,w.warehouse_code,pol.preferred_supplier_partner_id,pol.lead_time_days,pol.reorder_point,pol.reorder_qty,pol.safety_stock_qty HAVING available_qty<=pol.reorder_point ORDER BY p.code");return jdbcTemplate.queryForList(sql.toString(),a.toArray());
    }

    public List<Map<String,Object>> expiryAlerts(String warehouseId) {
        access.requireWorkcentre("stock-on-hand");access.requireWarehouse(warehouseId);
        StringBuilder sql=new StringBuilder("""
            SELECT b.product_id,p.code product_code,p.description product_description,b.warehouse_id,w.warehouse_code,b.storage_location_id,l.location_code,b.batch_no,b.stock_status,b.on_hand_qty,b.available_qty,lot.expiry_date,
                   DATEDIFF(lot.expiry_date,CURRENT_DATE) days_to_expiry,COALESCE(pol.expiry_warning_days,30) expiry_warning_days
              FROM stock_balance b JOIN product p ON p.id=b.product_id JOIN warehouse w ON w.id=b.warehouse_id JOIN storage_location l ON l.id=b.storage_location_id
              JOIN inventory_lot lot ON lot.product_id=b.product_id AND lot.batch_no=b.batch_no
              LEFT JOIN inventory_product_warehouse_policy pol ON pol.product_id=b.product_id AND pol.warehouse_id=b.warehouse_id
             WHERE b.on_hand_qty>0 AND lot.expiry_date IS NOT NULL AND lot.expiry_date<=DATE_ADD(CURRENT_DATE,INTERVAL COALESCE(pol.expiry_warning_days,30) DAY)
            """);List<Object>a=new ArrayList<>();if(StringUtils.hasText(warehouseId)){sql.append(" AND b.warehouse_id=?");a.add(warehouseId);}else appendWarehouseScope(sql,a,"b.warehouse_id");sql.append(" ORDER BY lot.expiry_date,p.code");return jdbcTemplate.queryForList(sql.toString(),a.toArray());
    }

    public List<Map<String,Object>> stockAgeing(String warehouseId) {
        access.requireWorkcentre("stock-on-hand");access.requireWarehouse(warehouseId);
        StringBuilder sql=new StringBuilder("""
            SELECT b.product_id,p.code product_code,p.description product_description,b.warehouse_id,w.warehouse_code,b.storage_location_id,l.location_code,b.batch_no,b.stock_status,b.on_hand_qty,b.inventory_value,
                   COALESCE(lot.created_at,first_receipt.first_received_at,b.last_movement_at) received_at,
                   DATEDIFF(CURRENT_DATE,DATE(COALESCE(lot.created_at,first_receipt.first_received_at,b.last_movement_at))) age_days,
                   CASE WHEN DATEDIFF(CURRENT_DATE,DATE(COALESCE(lot.created_at,first_receipt.first_received_at,b.last_movement_at)))<=30 THEN '0-30'
                        WHEN DATEDIFF(CURRENT_DATE,DATE(COALESCE(lot.created_at,first_receipt.first_received_at,b.last_movement_at)))<=60 THEN '31-60'
                        WHEN DATEDIFF(CURRENT_DATE,DATE(COALESCE(lot.created_at,first_receipt.first_received_at,b.last_movement_at)))<=90 THEN '61-90'
                        WHEN DATEDIFF(CURRENT_DATE,DATE(COALESCE(lot.created_at,first_receipt.first_received_at,b.last_movement_at)))<=180 THEN '91-180' ELSE '180+' END age_bucket
              FROM stock_balance b JOIN product p ON p.id=b.product_id JOIN warehouse w ON w.id=b.warehouse_id JOIN storage_location l ON l.id=b.storage_location_id
              LEFT JOIN inventory_lot lot ON lot.product_id=b.product_id AND lot.batch_no=b.batch_no AND b.batch_no<>''
              LEFT JOIN (SELECT product_id,warehouse_id,to_location_id,MIN(movement_at) first_received_at FROM stock_movement WHERE from_location_id IS NULL GROUP BY product_id,warehouse_id,to_location_id) first_receipt ON first_receipt.product_id=b.product_id AND first_receipt.warehouse_id=b.warehouse_id AND first_receipt.to_location_id=b.storage_location_id
             WHERE b.on_hand_qty<>0
            """);List<Object>a=new ArrayList<>();if(StringUtils.hasText(warehouseId)){sql.append(" AND b.warehouse_id=?");a.add(warehouseId);}else appendWarehouseScope(sql,a,"b.warehouse_id");sql.append(" ORDER BY age_days DESC,p.code");return jdbcTemplate.queryForList(sql.toString(),a.toArray());
    }

    public Map<String,Object> batchRecall(String batchNo) {
        access.requireWorkcentre("stock-movement");String batch=required(batchNo,"batchNo");Map<String,Object> result=new LinkedHashMap<>();
        List<String> scope=access.scopedWarehouseIds();
        StringBuilder stockSql=new StringBuilder("SELECT b.*,p.code product_code,w.warehouse_code,l.location_code FROM stock_balance b JOIN product p ON p.id=b.product_id JOIN warehouse w ON w.id=b.warehouse_id JOIN storage_location l ON l.id=b.storage_location_id WHERE b.batch_no=? AND b.on_hand_qty<>0");List<Object> stockArgs=new ArrayList<>();stockArgs.add(batch);appendWarehouseScope(stockSql,stockArgs,"b.warehouse_id");
        StringBuilder movementSql=new StringBuilder("SELECT m.*,p.code product_code,sw.warehouse_code source_warehouse_code,dw.warehouse_code destination_warehouse_code FROM stock_movement m JOIN product p ON p.id=m.product_id LEFT JOIN warehouse sw ON sw.id=COALESCE(m.source_warehouse_id,m.warehouse_id) LEFT JOIN warehouse dw ON dw.id=COALESCE(m.destination_warehouse_id,m.warehouse_id) WHERE m.batch_no=?");List<Object> movementArgs=new ArrayList<>();movementArgs.add(batch);appendDualWarehouseScope(movementSql,movementArgs,"COALESCE(m.source_warehouse_id,m.warehouse_id)","COALESCE(m.destination_warehouse_id,m.warehouse_id)");movementSql.append(" ORDER BY m.movement_at");
        List<Map<String,Object>> current=jdbcTemplate.queryForList(stockSql.toString(),stockArgs.toArray());
        List<Map<String,Object>> movements=jdbcTemplate.queryForList(movementSql.toString(),movementArgs.toArray());
        Set<String> visibleProducts=new LinkedHashSet<>();for(Map<String,Object> row:current)visibleProducts.add(text(row.get("product_id")));for(Map<String,Object> row:movements)visibleProducts.add(text(row.get("product_id")));
        if(scope.isEmpty())result.put("lot",jdbcTemplate.queryForList("SELECT lot.*,p.code product_code,p.description product_description FROM inventory_lot lot JOIN product p ON p.id=lot.product_id WHERE lot.batch_no=?",batch));
        else if(visibleProducts.isEmpty())result.put("lot",List.of());
        else {String placeholders=String.join(",",Collections.nCopies(visibleProducts.size(),"?"));List<Object> lotArgs=new ArrayList<>();lotArgs.add(batch);lotArgs.addAll(visibleProducts);result.put("lot",jdbcTemplate.queryForList("SELECT lot.*,p.code product_code,p.description product_description FROM inventory_lot lot JOIN product p ON p.id=lot.product_id WHERE lot.batch_no=? AND lot.product_id IN ("+placeholders+")",lotArgs.toArray()));}
        result.put("currentStock",current);result.put("movements",movements);return result;
    }

    public Map<String,Object> inventoryHealth(String warehouseId) {
        access.requireWorkcentre("inventory-audit");access.requireWarehouse(warehouseId);Map<String,Object> result=new LinkedHashMap<>();
        List<Object>a1=new ArrayList<>();StringBuilder q1=new StringBuilder("SELECT b.*,p.code product_code,w.warehouse_code,l.location_code FROM stock_balance b JOIN product p ON p.id=b.product_id JOIN warehouse w ON w.id=b.warehouse_id JOIN storage_location l ON l.id=b.storage_location_id WHERE b.on_hand_qty<0");if(StringUtils.hasText(warehouseId)){q1.append(" AND b.warehouse_id=?");a1.add(warehouseId);}else appendWarehouseScope(q1,a1,"b.warehouse_id");result.put("negativeStock",jdbcTemplate.queryForList(q1.toString(),a1.toArray()));
        List<Object>a2=new ArrayList<>();StringBuilder q2=new StringBuilder("SELECT b.*,p.code product_code,w.warehouse_code,l.location_code FROM stock_balance b JOIN product p ON p.id=b.product_id JOIN warehouse w ON w.id=b.warehouse_id JOIN storage_location l ON l.id=b.storage_location_id WHERE (b.reserved_qty<0 OR ABS(b.available_qty-(b.on_hand_qty-b.reserved_qty))>0.001 OR b.reserved_qty>b.on_hand_qty)");if(StringUtils.hasText(warehouseId)){q2.append(" AND b.warehouse_id=?");a2.add(warehouseId);}else appendWarehouseScope(q2,a2,"b.warehouse_id");result.put("reservationMismatch",jdbcTemplate.queryForList(q2.toString(),a2.toArray()));
        List<Object>a3=new ArrayList<>();StringBuilder q3=new StringBuilder("SELECT b.*,p.code product_code,w.warehouse_code,l.location_code,lot.expiry_date FROM stock_balance b JOIN inventory_lot lot ON lot.product_id=b.product_id AND lot.batch_no=b.batch_no JOIN product p ON p.id=b.product_id JOIN warehouse w ON w.id=b.warehouse_id JOIN storage_location l ON l.id=b.storage_location_id WHERE lot.expiry_date<CURRENT_DATE AND b.stock_status='UNRESTRICTED' AND b.on_hand_qty>0");if(StringUtils.hasText(warehouseId)){q3.append(" AND b.warehouse_id=?");a3.add(warehouseId);}else appendWarehouseScope(q3,a3,"b.warehouse_id");result.put("expiredUnrestricted",jdbcTemplate.queryForList(q3.toString(),a3.toArray()));
        List<Object>a4=new ArrayList<>();StringBuilder q4=new StringBuilder("SELECT d.*,DATEDIFF(CURRENT_DATE,DATE(d.posted_at)) days_in_transit FROM inventory_document d WHERE d.document_type='WAREHOUSE_TRANSFER' AND d.status IN ('IN_TRANSIT','PARTIALLY_RECEIVED') AND d.posted_at<DATE_SUB(NOW(),INTERVAL 7 DAY)");if(StringUtils.hasText(warehouseId)){q4.append(" AND (d.warehouse_id=? OR d.destination_warehouse_id=?)");a4.add(warehouseId);a4.add(warehouseId);}else appendDualWarehouseScope(q4,a4,"d.warehouse_id","d.destination_warehouse_id");q4.append(" ORDER BY d.posted_at");result.put("staleTransfers",jdbcTemplate.queryForList(q4.toString(),a4.toArray()));
        return result;
    }

    public List<Map<String,Object>> uomConversions(String productId) {
        access.requireWorkcentre("inventory-setup");
        return jdbcTemplate.queryForList("SELECT c.*,COALESCE(NULLIF(TRIM(p.uom),''),'EA') base_uom FROM product_uom_conversion c JOIN product p ON p.id=c.product_id WHERE c.product_id=? ORDER BY c.uom",productId);
    }

    @Transactional
    public Map<String,Object> saveUomConversion(InventoryDtos.UomConversionRequest request) {
        access.requireWorkcentre("inventory-setup");
        String productId=required(request.getProductId(),"productId");String uom=required(request.getUom(),"uom").toUpperCase(Locale.ROOT);
        if(request.getConversionFactorToBase()==null||request.getConversionFactorToBase().compareTo(BigDecimal.ZERO)<=0)throw new IllegalArgumentException("conversionFactorToBase must be greater than zero");
        String actor=access.actorId();
        jdbcTemplate.update("INSERT INTO product_uom_conversion(product_id,uom,conversion_factor_to_base,description,active,created_at,created_by,updated_at,updated_by) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE conversion_factor_to_base=VALUES(conversion_factor_to_base),description=VALUES(description),active=VALUES(active),updated_at=VALUES(updated_at),updated_by=VALUES(updated_by)",productId,uom,request.getConversionFactorToBase(),request.getDescription(),request.getActive()==null||request.getActive(),now(),actor,now(),actor);
        return jdbcTemplate.queryForMap("SELECT * FROM product_uom_conversion WHERE product_id=? AND uom=?",productId,uom);
    }

    public List<Map<String,Object>> warehouseScopeOptions() {
        access.requireWorkcentre("inventory-setup");
        return jdbcTemplate.queryForList("SELECT id,warehouse_code,name,status FROM warehouse WHERE UPPER(COALESCE(status,'ACTIVE'))='ACTIVE' ORDER BY warehouse_code");
    }

    public List<Map<String,Object>> roleWarehouseScope(String roleId) {
        access.requireWorkcentre("inventory-setup");
        return jdbcTemplate.queryForList("SELECT w.id,w.warehouse_code,w.name FROM role_warehouse_access a JOIN warehouse w ON w.id=a.warehouse_id WHERE a.role_id=? ORDER BY w.warehouse_code", roleId);
    }

    @Transactional
    public List<Map<String,Object>> saveRoleWarehouseScope(String roleId,List<String> warehouseIds) {
        access.requireWorkcentre("inventory-setup");
        String actor=access.actorId();
        jdbcTemplate.update("DELETE FROM role_warehouse_access WHERE role_id=?",roleId);
        if(warehouseIds!=null) for(String warehouseId:new LinkedHashSet<>(warehouseIds)){
            if(!StringUtils.hasText(warehouseId))continue;
            Integer exists=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouse WHERE id=?",Integer.class,warehouseId);
            if(exists==null||exists!=1)throw new IllegalArgumentException("Warehouse not found: "+warehouseId);
            jdbcTemplate.update("INSERT INTO role_warehouse_access(role_id,warehouse_id,created_at,created_by) VALUES(?,?,?,?)",roleId,warehouseId,now(),actor);
        }
        return roleWarehouseScope(roleId);
    }

    public List<Map<String,Object>> userWarehouseScope(String userId) {
        access.requireWorkcentre("inventory-setup");
        return jdbcTemplate.queryForList("SELECT w.id,w.warehouse_code,w.name FROM user_warehouse_access a JOIN warehouse w ON w.id=a.warehouse_id WHERE a.user_id=? ORDER BY w.warehouse_code", userId);
    }

    @Transactional
    public List<Map<String,Object>> saveUserWarehouseScope(String userId,List<String> warehouseIds) {
        access.requireWorkcentre("inventory-setup");
        String actor=access.actorId();
        jdbcTemplate.update("DELETE FROM user_warehouse_access WHERE user_id=?",userId);
        if(warehouseIds!=null) for(String warehouseId:new LinkedHashSet<>(warehouseIds)){
            if(!StringUtils.hasText(warehouseId))continue;
            Integer exists=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouse WHERE id=?",Integer.class,warehouseId);
            if(exists==null||exists!=1)throw new IllegalArgumentException("Warehouse not found: "+warehouseId);
            jdbcTemplate.update("INSERT INTO user_warehouse_access(user_id,warehouse_id,created_at,created_by) VALUES(?,?,?,?)",userId,warehouseId,now(),actor);
        }
        return userWarehouseScope(userId);
    }


    private void requireWorkcentreForType(String type) {
        String wc=switch(type.toUpperCase(Locale.ROOT)){
            case "GOODS_RECEIPT_PO","GOODS_RECEIPT_DIRECT" -> "goods-receipt";
            case "PUTAWAY" -> "putaway";
            case "QUALITY_HOLD","QUALITY_RELEASE","QUALITY_REJECT","QUARANTINE","QUARANTINE_RELEASE" -> "quality-inspection";
            case "REPLENISHMENT" -> "stock-replenishment";
            case "BIN_TRANSFER" -> "stock-transfer";
            case "TRANSFER_OUT","TRANSFER_IN","WAREHOUSE_TRANSFER" -> "warehouse-transfer";
            case "PICK","PICK_REVERSAL" -> "stock-picking";
            case "SALES_ISSUE","INTERNAL_CONSUMPTION","FUNERAL_SERVICE_ISSUE","SERVICE_ORDER_ISSUE","PRODUCTION_ISSUE","TOMBSTONE_INSTALLATION_ISSUE" -> "stock-dispatch";
            case "CUSTOMER_RETURN_RECEIPT","RETURN_TO_SUPPLIER" -> "stock-returns";
            case "STOCKTAKE_GAIN","STOCKTAKE_LOSS" -> "stock-count";
            case "ADJUSTMENT_IN","ADJUSTMENT_OUT" -> "stock-adjustment";
            case "DAMAGE_TRANSFER","EXPIRY_TRANSFER","SCRAP_TRANSFER","DISPOSAL_ISSUE" -> "stock-writeoff";
            case "MOVEMENT_REVERSAL" -> "stock-reversal";
            default -> "stock-movement";
        }; access.requireWorkcentre(wc);
    }

    private void appendWarehouseScope(StringBuilder sql,List<Object> args,String column){
        List<String> scope=access.scopedWarehouseIds();if(scope.isEmpty())return;sql.append(" AND ").append(column).append(" IN (").append(String.join(",",Collections.nCopies(scope.size(),"?"))).append(")");args.addAll(scope);
    }

    private void appendDualWarehouseScope(StringBuilder sql,List<Object> args,String firstColumn,String secondColumn){
        List<String> scope=access.scopedWarehouseIds();if(scope.isEmpty())return;String placeholders=String.join(",",Collections.nCopies(scope.size(),"?"));sql.append(" AND (").append(firstColumn).append(" IN (").append(placeholders).append(") OR ").append(secondColumn).append(" IN (").append(placeholders).append("))");args.addAll(scope);args.addAll(scope);
    }

    private BaseQuantity toBaseQuantity(String productId,BigDecimal quantity,String requestedUom){
        String input=defaultText(requestedUom,"EA").toUpperCase(Locale.ROOT);
        List<String> bases=jdbcTemplate.query("SELECT COALESCE(NULLIF(TRIM(uom),''),'EA') FROM product WHERE id=?",(rs,rowNum)->rs.getString(1),productId);
        if(bases.isEmpty())throw new IllegalArgumentException("Product not found: "+productId);
        String base=defaultText(bases.get(0),"EA").toUpperCase(Locale.ROOT);
        if(base.equals(input))return new BaseQuantity(quantity.setScale(3,java.math.RoundingMode.HALF_UP),base);
        List<BigDecimal> factors=jdbcTemplate.query("SELECT conversion_factor_to_base FROM product_uom_conversion WHERE product_id=? AND UPPER(uom)=? AND active=1",(rs,rowNum)->rs.getBigDecimal(1),productId,input);
        if(factors.isEmpty()||factors.get(0)==null||factors.get(0).compareTo(BigDecimal.ZERO)<=0)throw new IllegalArgumentException("No active UOM conversion from "+input+" to "+base+" for product "+productId);
        return new BaseQuantity(quantity.multiply(factors.get(0)).setScale(3,java.math.RoundingMode.HALF_UP),base);
    }

    private String nextNumber(String object,String fallback){try{String n=numberRangeService.generateNumber(object);if(StringUtils.hasText(n))return n;}catch(NumberRangeObjectNotFound ignored){}catch(Exception ignored){}return fallback+"-"+System.currentTimeMillis();}
    private String required(String v,String field){if(!StringUtils.hasText(v))throw new IllegalArgumentException(field+" is required");return v;}
    private String defaultText(String v,String d){return StringUtils.hasText(v)?v.trim():d;}
    private String empty(String v){return StringUtils.hasText(v)?v.trim():null;}
    private String text(Object v){return v==null?null:v.toString();}
    private BigDecimal decimal(Object v){return v==null?BigDecimal.ZERO:new BigDecimal(v.toString());}
    private BigDecimal nvl(BigDecimal v){return v==null?BigDecimal.ZERO:v;}
    private String uuid(){return UUID.randomUUID().toString().replace("-","");}
    private Timestamp now(){return Timestamp.valueOf(LocalDateTime.now());}
    private record BaseQuantity(BigDecimal quantity,String uom){}
}
