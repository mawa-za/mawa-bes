package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.v2.stock.InventoryDtos;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.service.NumberRangeService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InventoryPostingService {
    private static final Set<String> MOVEMENT_TYPES = Set.of(
            "GOODS_RECEIPT_PO","GOODS_RECEIPT_DIRECT","CUSTOMER_RETURN_RECEIPT","PRODUCTION_RECEIPT",
            "PUTAWAY","QUALITY_HOLD","QUALITY_RELEASE","QUALITY_REJECT","QUARANTINE","QUARANTINE_RELEASE",
            "BIN_TRANSFER","REPLENISHMENT","PICK","PICK_REVERSAL","SALES_ISSUE","INTERNAL_CONSUMPTION",
            "FUNERAL_SERVICE_ISSUE","SERVICE_ORDER_ISSUE","PRODUCTION_ISSUE","TOMBSTONE_INSTALLATION_ISSUE",
            "TRANSFER_OUT","TRANSFER_IN","RETURN_TO_SUPPLIER","DAMAGE_TRANSFER","EXPIRY_TRANSFER","SCRAP_TRANSFER",
            "DISPOSAL_ISSUE","STOCKTAKE_GAIN","STOCKTAKE_LOSS","ADJUSTMENT_IN","ADJUSTMENT_OUT","OTHER_RECEIPT",
            "OTHER_ISSUE","MOVEMENT_REVERSAL","ASSEMBLY_RECEIPT","ASSEMBLY_CONSUMPTION","DISASSEMBLY_RECEIPT","DISASSEMBLY_CONSUMPTION"
    );
    private static final Set<String> STOCK_STATUSES = Set.of(
            "UNRESTRICTED","QUALITY","QUARANTINE","BLOCKED","DAMAGED","EXPIRED","SCRAP","IN_TRANSIT","RETURNS"
    );

    private final JdbcTemplate jdbcTemplate;
    private final NumberRangeService numberRangeService;
    private final InventoryAccessService accessService;

    public InventoryPostingService(JdbcTemplate jdbcTemplate, NumberRangeService numberRangeService, InventoryAccessService accessService) {
        this.jdbcTemplate = jdbcTemplate;
        this.numberRangeService = numberRangeService;
        this.accessService = accessService;
    }

    @Transactional
    public Map<String,Object> post(InventoryDtos.PostingRequest request) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one inventory posting line is required");
        }
        String movementType = normalized(request.getMovementType(), "movementType");
        if (!MOVEMENT_TYPES.contains(movementType)) throw new IllegalArgumentException("Unsupported inventory movement type: " + movementType);
        String idempotencyKey = required(request.getIdempotencyKey(), "idempotencyKey").trim();
        List<Map<String,Object>> existing = jdbcTemplate.queryForList("SELECT * FROM inventory_posting WHERE idempotency_key=?", idempotencyKey);
        if (!existing.isEmpty()) return existing.get(0);

        String actor = accessService.actorId();
        String postingId = uuid();
        String postingNo = nextNumber("INVENTORY_POSTING", "IP");
        LocalDate postingDate = request.getPostingDate() == null ? LocalDate.now() : request.getPostingDate();
        validatePostingDate(postingDate);
        jdbcTemplate.update("INSERT INTO inventory_posting(id,inventory_document_id,posting_no,movement_type,reference_type,reference_id,reference_no,posting_date,idempotency_key,notes,created_at,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                postingId, emptyToNull(request.getDocumentId()), postingNo, movementType, emptyToNull(request.getReferenceType()), emptyToNull(request.getReferenceId()), emptyToNull(request.getReferenceNo()), Date.valueOf(postingDate), idempotencyKey, request.getNotes(), now(), actor);

        int index = 0;
        for (InventoryDtos.PostingLineRequest line : request.getLines()) {
            index++;
            postLine(postingId, postingNo, movementType, request, line, actor, index);
        }
        if (StringUtils.hasText(request.getDocumentId())) {
            jdbcTemplate.update("UPDATE inventory_document SET status='POSTED',posting_date=?,posted_at=?,posted_by=?,updated_at=?,updated_by=? WHERE id=?",
                    Date.valueOf(postingDate), now(), actor, now(), actor, request.getDocumentId());
        }
        return jdbcTemplate.queryForMap("SELECT * FROM inventory_posting WHERE id=?", postingId);
    }

    private void postLine(String postingId, String postingNo, String movementType, InventoryDtos.PostingRequest request,
                          InventoryDtos.PostingLineRequest line, String actor, int lineIndex) {
        String productId = required(line.getProductId(), "productId");
        BigDecimal transactionQty = positive(line.getQuantity(), "quantity");
        String transactionUom = defaultText(line.getUom(), "EA").toUpperCase(Locale.ROOT);
        QuantityConversion conversion = convertToBase(productId, transactionQty, transactionUom, line.getUnitCost());
        BigDecimal qty = conversion.baseQuantity();
        String uom = conversion.baseUom();
        String batchNo = defaultText(line.getBatchNo(), "");
        String sourceStatus = stockStatus(line.getSourceStockStatus(), "UNRESTRICTED");
        String destinationStatus = stockStatus(line.getDestinationStockStatus(), "UNRESTRICTED");
        String sourceWarehouse = firstText(line.getSourceWarehouseId(), line.getWarehouseId());
        String destinationWarehouse = firstText(line.getDestinationWarehouseId(), line.getWarehouseId());
        String sourceLocation = emptyToNull(line.getSourceLocationId());
        String destinationLocation = emptyToNull(line.getDestinationLocationId());
        List<String> serialNumbers = normalizedSerials(line.getSerialNumbers());

        BigDecimal unitCost = conversion.baseUnitCost();
        if (sourceLocation != null) {
            required(sourceWarehouse, "sourceWarehouseId");
            accessService.requireWarehouse(sourceWarehouse);
            LocationRules sourceRules = locationRules(sourceWarehouse, sourceLocation);
            validateTrackingRequirements(sourceRules, line, batchNo, false);
            assertNotFrozen(movementType, sourceWarehouse, sourceLocation);
            SourceBalance source = lockSource(productId, sourceWarehouse, sourceLocation, batchNo, sourceStatus);
            if (source == null) throw new IllegalStateException("No source stock exists for product " + productId + " at the selected location");
            validateMinimumShelfLife(movementType, productId, sourceWarehouse, batchNo);
            if (unitCost.compareTo(BigDecimal.ZERO) == 0) unitCost = source.unitCost();
            boolean allowNegative = locationAllowsNegative(sourceLocation);
            BigDecimal usable = source.availableQty();
            if (!allowNegative && usable.compareTo(qty) < 0) {
                throw new IllegalStateException("Insufficient available stock for product " + productId + ". Available " + usable + ", requested " + qty);
            }
            jdbcTemplate.update("UPDATE stock_balance SET on_hand_qty=on_hand_qty-?, available_qty=available_qty-?, inventory_value=(on_hand_qty-?)*unit_cost,last_movement_at=?,updated_at=?,updated_by=? WHERE id=?",
                    qty, qty, qty, now(), now(), actor, source.id());
        }

        if (destinationLocation != null) {
            required(destinationWarehouse, "destinationWarehouseId");
            accessService.requireWarehouse(destinationWarehouse);
            LocationRules destinationRules = locationRules(destinationWarehouse, destinationLocation);
            validateTrackingRequirements(destinationRules, line, batchNo, true);
            if (destinationRules.requiresQualityRelease() && "UNRESTRICTED".equals(destinationStatus)) {
                throw new IllegalStateException("Destination location requires quality release; post stock to QUALITY status first");
            }
            assertNotFrozen(movementType, destinationWarehouse, destinationLocation);
            credit(productId, destinationWarehouse, destinationLocation, batchNo, destinationStatus, qty, uom, unitCost, actor);
            if (StringUtils.hasText(batchNo)) registerLot(productId, batchNo, line.getExpiryDate(), request, actor);
        }

        if (sourceLocation == null && destinationLocation == null) {
            throw new IllegalArgumentException("Each inventory posting line requires a sourceLocationId, destinationLocationId, or both");
        }
        boolean serialRequired = (sourceLocation != null && locationRules(sourceWarehouse, sourceLocation).requiresSerialNumber())
                || (destinationLocation != null && locationRules(destinationWarehouse, destinationLocation).requiresSerialNumber());
        if (serialRequired) {
            int expected;
            try { expected = qty.stripTrailingZeros().intValueExact(); }
            catch (ArithmeticException e) { throw new IllegalArgumentException("Serial-controlled stock quantity must be a whole number"); }
            if (serialNumbers.size() != expected) throw new IllegalArgumentException("Exactly " + expected + " serial number(s) are required for serial-controlled stock");
            validateSourceSerials(productId, sourceWarehouse, sourceLocation, sourceStatus, serialNumbers);
        }
        String movementId = uuid();
        String movementNo = nextNumber("STOCK_MOVEMENT", "STM");
        BigDecimal value = qty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
        String movementIdempotency = request.getIdempotencyKey() + ":" + lineIndex;
        jdbcTemplate.update("""
                INSERT INTO stock_movement(id,posting_id,movement_no,movement_type,reference_type,reference_id,reference_no,
                  product_id,warehouse_id,source_warehouse_id,destination_warehouse_id,from_location_id,to_location_id,quantity,transaction_quantity,uom,transaction_uom,batch_no,stock_status_from,stock_status_to,
                  unit_cost,movement_value,idempotency_key,movement_at,processed_by,notes,created_at,created_by)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                movementId, postingId, movementNo, movementType, emptyToNull(request.getReferenceType()), emptyToNull(request.getReferenceId()), emptyToNull(request.getReferenceNo()),
                productId, firstText(destinationWarehouse, sourceWarehouse), sourceWarehouse, destinationWarehouse, sourceLocation, destinationLocation, qty, transactionQty, uom, transactionUom, batchNo, sourceLocation == null ? null : sourceStatus,
                destinationLocation == null ? null : destinationStatus, unitCost, value, movementIdempotency, now(), actor, request.getNotes(), now(), actor);
        if (!serialNumbers.isEmpty()) {
            updateSerials(movementId, movementType, productId, serialNumbers, sourceWarehouse, sourceLocation, sourceStatus, destinationWarehouse, destinationLocation, destinationStatus, batchNo, actor);
        }
    }

    private SourceBalance lockSource(String productId, String warehouseId, String locationId, String batchNo, String status) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT id,on_hand_qty,reserved_qty,available_qty,unit_cost FROM stock_balance WHERE product_id=? AND warehouse_id=? AND storage_location_id=? AND batch_no=? AND stock_status=? FOR UPDATE",
                productId, warehouseId, locationId, batchNo, status);
        if (rows.isEmpty()) return null;
        Map<String,Object> row = rows.get(0);
        return new SourceBalance(text(row.get("id")), decimal(row.get("on_hand_qty")), decimal(row.get("reserved_qty")), decimal(row.get("available_qty")), decimal(row.get("unit_cost")));
    }

    private void credit(String productId, String warehouseId, String locationId, String batchNo, String status,
                        BigDecimal qty, String uom, BigDecimal incomingCost, String actor) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT id,on_hand_qty,reserved_qty,unit_cost FROM stock_balance WHERE product_id=? AND warehouse_id=? AND storage_location_id=? AND batch_no=? AND stock_status=? FOR UPDATE",
                productId, warehouseId, locationId, batchNo, status);
        if (rows.isEmpty()) {
            BigDecimal value = qty.multiply(incomingCost).setScale(2, RoundingMode.HALF_UP);
            jdbcTemplate.update("INSERT INTO stock_balance(id,product_id,warehouse_id,storage_location_id,batch_no,stock_status,on_hand_qty,reserved_qty,available_qty,uom,minimum_qty,unit_cost,inventory_value,last_movement_at,updated_at,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    uuid(), productId, warehouseId, locationId, batchNo, status, qty, BigDecimal.ZERO, qty, uom, BigDecimal.ZERO, incomingCost, value, now(), now(), actor);
            return;
        }
        Map<String,Object> row = rows.get(0);
        BigDecimal oldQty = decimal(row.get("on_hand_qty"));
        BigDecimal oldCost = decimal(row.get("unit_cost"));
        BigDecimal newQty = oldQty.add(qty);
        BigDecimal weightedCost = newQty.compareTo(BigDecimal.ZERO) == 0 ? incomingCost : oldQty.multiply(oldCost).add(qty.multiply(incomingCost)).divide(newQty,4,RoundingMode.HALF_UP);
        jdbcTemplate.update("UPDATE stock_balance SET on_hand_qty=?,available_qty=?-reserved_qty,unit_cost=?,inventory_value=?*?,uom=?,last_movement_at=?,updated_at=?,updated_by=? WHERE id=?",
                newQty, newQty, weightedCost, newQty, weightedCost, uom, now(), now(), actor, row.get("id"));
    }


    private QuantityConversion convertToBase(String productId, BigDecimal quantity, String transactionUom, BigDecimal transactionUnitCost) {
        List<String> baseRows = jdbcTemplate.query("SELECT COALESCE(NULLIF(TRIM(uom),''),'EA') FROM product WHERE id=?", (rs,rowNum) -> rs.getString(1), productId);
        if (baseRows.isEmpty()) throw new IllegalArgumentException("Product not found: " + productId);
        String baseUom = defaultText(baseRows.get(0), "EA").toUpperCase(Locale.ROOT);
        BigDecimal factor = BigDecimal.ONE;
        if (!baseUom.equalsIgnoreCase(transactionUom)) {
            List<BigDecimal> factors = jdbcTemplate.query("SELECT conversion_factor_to_base FROM product_uom_conversion WHERE product_id=? AND UPPER(uom)=? AND active=1", (rs,rowNum) -> rs.getBigDecimal(1), productId, transactionUom.toUpperCase(Locale.ROOT));
            if (factors.isEmpty() || factors.get(0) == null || factors.get(0).compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("No active UOM conversion from " + transactionUom + " to base UOM " + baseUom + " for product " + productId);
            }
            factor = factors.get(0);
        }
        BigDecimal baseQuantity = quantity.multiply(factor).setScale(3, RoundingMode.HALF_UP);
        BigDecimal baseCost = transactionUnitCost == null ? BigDecimal.ZERO : transactionUnitCost.divide(factor, 4, RoundingMode.HALF_UP);
        return new QuantityConversion(baseQuantity, baseUom, baseCost);
    }

    private void validateMinimumShelfLife(String movementType,String productId,String warehouseId,String batchNo) {
        if (!StringUtils.hasText(batchNo)) return;
        if (!Set.of("PICK","SALES_ISSUE","FUNERAL_SERVICE_ISSUE","SERVICE_ORDER_ISSUE","INTERNAL_CONSUMPTION","TOMBSTONE_INSTALLATION_ISSUE").contains(movementType)) return;
        List<Integer> values=jdbcTemplate.query("SELECT COALESCE(minimum_shelf_life_days,0) FROM inventory_product_warehouse_policy WHERE product_id=? AND warehouse_id=?",(rs,rowNum)->rs.getInt(1),productId,warehouseId);
        int minimum=values.isEmpty()?0:Math.max(values.get(0),0);
        if(minimum<=0)return;
        List<Date> expiry=jdbcTemplate.query("SELECT expiry_date FROM inventory_lot WHERE product_id=? AND batch_no=? AND expiry_date IS NOT NULL",(rs,rowNum)->rs.getDate(1),productId,batchNo);
        if(!expiry.isEmpty() && expiry.get(0).toLocalDate().isBefore(LocalDate.now().plusDays(minimum))) {
            throw new IllegalStateException("Batch " + batchNo + " does not meet the configured minimum remaining shelf life of " + minimum + " days");
        }
    }

    private void validatePostingDate(LocalDate postingDate) {
        LocalDate today = LocalDate.now();
        if (postingDate.isAfter(today)) throw new IllegalArgumentException("Inventory posting date cannot be in the future");
        int maxBackdateDays = settingInt("MAX_BACKDATE_DAYS", 31);
        if (maxBackdateDays >= 0 && postingDate.isBefore(today.minusDays(maxBackdateDays))) {
            throw new IllegalStateException("Inventory posting date exceeds the configured backdate limit of " + maxBackdateDays + " days");
        }
        String closedThrough = setting("CLOSED_THROUGH_DATE");
        if (StringUtils.hasText(closedThrough)) {
            LocalDate closed = LocalDate.parse(closedThrough.trim());
            if (!postingDate.isAfter(closed)) throw new IllegalStateException("Inventory posting period is closed through " + closed);
        }
    }

    private int settingInt(String setting, int fallback) {
        String value = setting(setting);
        if (!StringUtils.hasText(value)) return fallback;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) { return fallback; }
    }

    private String setting(String setting) {
        List<String> values = jdbcTemplate.query("SELECT value FROM settings WHERE attribute='INVENTORY' AND setting=?", (rs,rowNum) -> rs.getString(1), setting);
        return values.isEmpty() ? null : values.get(0);
    }

    private void registerLot(String productId, String batchNo, LocalDate expiryDate, InventoryDtos.PostingRequest request, String actor) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT id,expiry_date FROM inventory_lot WHERE product_id=? AND batch_no=? FOR UPDATE", productId, batchNo);
        if (rows.isEmpty()) {
            jdbcTemplate.update("INSERT INTO inventory_lot(id,product_id,batch_no,expiry_date,source_receipt_id,status,created_at,created_by,updated_at,updated_by) VALUES(?,?,?,?,?,'ACTIVE',?,?,?,?)",
                    uuid(), productId, batchNo, expiryDate == null ? null : Date.valueOf(expiryDate),
                    StringUtils.hasText(request.getReferenceId()) ? request.getReferenceId() : request.getDocumentId(), now(), actor, now(), actor);
        } else if (expiryDate != null) {
            jdbcTemplate.update("UPDATE inventory_lot SET expiry_date=COALESCE(expiry_date,?),updated_at=?,updated_by=? WHERE id=?", Date.valueOf(expiryDate), now(), actor, rows.get(0).get("id"));
        }
    }

    private void assertNotFrozen(String movementType, String warehouseId, String locationId) {
        if ("STOCKTAKE_GAIN".equals(movementType) || "STOCKTAKE_LOSS".equals(movementType)) return;
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_stock_count
                 WHERE warehouse_id=? AND freeze_stock=1
                   AND status IN ('OPEN','COUNTING','PENDING_APPROVAL')
                   AND (storage_location_id IS NULL OR storage_location_id=?)
                """, Integer.class, warehouseId, locationId);
        if (count != null && count > 0) {
            throw new IllegalStateException("Inventory is frozen for an active stock count at the selected warehouse/location");
        }
    }

    private LocationRules locationRules(String warehouseId, String locationId) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT COALESCE(t.requires_batch,0) requires_batch,COALESCE(t.requires_expiry_date,0) requires_expiry_date,COALESCE(t.requires_serial_number,0) requires_serial_number,COALESCE(t.requires_quality_release,0) requires_quality_release FROM storage_location l JOIN warehouse w ON w.id=l.warehouse_id LEFT JOIN storage_location_type t ON t.code=l.location_type WHERE l.id=? AND l.warehouse_id=? AND UPPER(COALESCE(l.status,'ACTIVE'))='ACTIVE' AND UPPER(COALESCE(w.status,'ACTIVE'))='ACTIVE'", locationId, warehouseId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Storage location is not active or does not belong to the selected warehouse");
        Map<String,Object> row=rows.get(0);
        return new LocationRules(bool(row.get("requires_batch")),bool(row.get("requires_expiry_date")),bool(row.get("requires_serial_number")),bool(row.get("requires_quality_release")));
    }

    private void validateTrackingRequirements(LocationRules rules, InventoryDtos.PostingLineRequest line, String batchNo, boolean destination) {
        if (rules.requiresBatch() && !StringUtils.hasText(batchNo)) throw new IllegalArgumentException("Batch number is required for the selected storage location");
        if (destination && rules.requiresExpiryDate() && line.getExpiryDate()==null && StringUtils.hasText(batchNo)) {
            Integer existing=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_lot WHERE product_id=? AND batch_no=? AND expiry_date IS NOT NULL",Integer.class,line.getProductId(),batchNo);
            if(existing==null||existing==0) throw new IllegalArgumentException("Expiry date is required for the selected storage location");
        }
        if (destination && rules.requiresExpiryDate() && !StringUtils.hasText(batchNo) && line.getExpiryDate()==null) throw new IllegalArgumentException("Expiry date is required for the selected storage location");
    }

    private List<String> normalizedSerials(List<String> serials) {
        if(serials==null||serials.isEmpty()) return List.of();
        LinkedHashSet<String> result=new LinkedHashSet<>();
        for(String serial:serials){if(StringUtils.hasText(serial))result.add(serial.trim());}
        if(result.size()!=serials.stream().filter(StringUtils::hasText).count()) throw new IllegalArgumentException("Duplicate serial numbers are not allowed in one inventory posting line");
        return List.copyOf(result);
    }

    private void validateSourceSerials(String productId,String warehouseId,String locationId,String stockStatus,List<String> serials) {
        if(serials.isEmpty()||locationId==null)return;
        for(String serial:serials){
            Integer count=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_serial WHERE product_id=? AND serial_no=? AND warehouse_id=? AND storage_location_id=? AND stock_status=? AND status='IN_STOCK'",Integer.class,productId,serial,warehouseId,locationId,stockStatus);
            if(count==null||count!=1)throw new IllegalStateException("Serial number "+serial+" is not available at the selected source location/status");
        }
    }

    private void updateSerials(String movementId,String movementType,String productId,List<String> serials,String sourceWarehouse,String sourceLocation,String sourceStatus,String destinationWarehouse,String destinationLocation,String destinationStatus,String batchNo,String actor) {
        for(String serial:serials){
            List<Map<String,Object>> existing=jdbcTemplate.queryForList("SELECT * FROM inventory_serial WHERE product_id=? AND serial_no=? FOR UPDATE",productId,serial);
            if(sourceLocation==null){
                if(!existing.isEmpty() && "IN_STOCK".equalsIgnoreCase(text(existing.get(0).get("status")))) throw new IllegalStateException("Serial number "+serial+" is already in stock");
                if(existing.isEmpty()) jdbcTemplate.update("INSERT INTO inventory_serial(id,product_id,serial_no,warehouse_id,storage_location_id,stock_status,batch_no,status,last_movement_id,created_at,created_by,updated_at,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",uuid(),productId,serial,destinationWarehouse,destinationLocation,destinationStatus,batchNo,"IN_STOCK",movementId,now(),actor,now(),actor);
                else jdbcTemplate.update("UPDATE inventory_serial SET warehouse_id=?,storage_location_id=?,stock_status=?,batch_no=?,status='IN_STOCK',last_movement_id=?,updated_at=?,updated_by=? WHERE id=?",destinationWarehouse,destinationLocation,destinationStatus,batchNo,movementId,now(),actor,existing.get(0).get("id"));
            } else if(destinationLocation==null){
                String nextStatus="TRANSFER_OUT".equals(movementType)?"IN_TRANSIT":"ISSUED";
                jdbcTemplate.update("UPDATE inventory_serial SET warehouse_id=NULL,storage_location_id=NULL,status=?,last_movement_id=?,updated_at=?,updated_by=? WHERE product_id=? AND serial_no=?",nextStatus,movementId,now(),actor,productId,serial);
            } else {
                jdbcTemplate.update("UPDATE inventory_serial SET warehouse_id=?,storage_location_id=?,stock_status=?,batch_no=?,status='IN_STOCK',last_movement_id=?,updated_at=?,updated_by=? WHERE product_id=? AND serial_no=?",destinationWarehouse,destinationLocation,destinationStatus,batchNo,movementId,now(),actor,productId,serial);
            }
        }
    }

    private boolean bool(Object value){if(value instanceof Boolean b)return b;if(value instanceof Number n)return n.intValue()!=0;return value!=null&&Boolean.parseBoolean(value.toString());}

    private boolean locationAllowsNegative(String locationId) {
        Boolean value = jdbcTemplate.queryForObject("SELECT COALESCE(t.allow_negative_stock,0) FROM storage_location l LEFT JOIN storage_location_type t ON t.code=l.location_type WHERE l.id=?", Boolean.class, locationId);
        return Boolean.TRUE.equals(value);
    }

    private String stockStatus(String value, String fallback) {
        String normalized = defaultText(value, fallback).toUpperCase(Locale.ROOT);
        if (!STOCK_STATUSES.contains(normalized)) throw new IllegalArgumentException("Unsupported stock status: " + normalized);
        return normalized;
    }

    private String nextNumber(String object, String fallback) {
        try { String n = numberRangeService.generateNumber(object); if (StringUtils.hasText(n)) return n; }
        catch (NumberRangeObjectNotFound ignored) { } catch (Exception ignored) { }
        return fallback + "-" + System.currentTimeMillis();
    }

    private String normalized(String value, String field) { return required(value, field).trim().toUpperCase(Locale.ROOT); }
    private String required(String value, String field) { if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " is required"); return value; }
    private BigDecimal positive(BigDecimal value, String field) { if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(field + " must be greater than zero"); return value; }
    private String defaultText(String value, String fallback) { return StringUtils.hasText(value) ? value.trim() : fallback; }
    private String firstText(String first, String second) { return StringUtils.hasText(first) ? first.trim() : (StringUtils.hasText(second) ? second.trim() : null); }
    private String emptyToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String text(Object value) { return value == null ? null : value.toString(); }
    private BigDecimal decimal(Object value) { return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()); }
    private String uuid() { return UUID.randomUUID().toString().replace("-", ""); }
    private Timestamp now() { return Timestamp.valueOf(LocalDateTime.now()); }
    private record SourceBalance(String id, BigDecimal onHandQty, BigDecimal reservedQty, BigDecimal availableQty, BigDecimal unitCost) {}
    private record QuantityConversion(BigDecimal baseQuantity, String baseUom, BigDecimal baseUnitCost) {}
    private record LocationRules(boolean requiresBatch, boolean requiresExpiryDate, boolean requiresSerialNumber, boolean requiresQualityRelease) {}
}
