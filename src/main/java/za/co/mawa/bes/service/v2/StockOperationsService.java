package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.stock.StockDtos;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.service.NumberRangeService;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StockOperationsService {
    private final JdbcTemplate jdbcTemplate;
    private final NumberRangeService numberRangeService;

    public StockOperationsService(JdbcTemplate jdbcTemplate, NumberRangeService numberRangeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.numberRangeService = numberRangeService;
    }

    public List<Map<String, Object>> getWarehouses(String status) {
        if (hasText(status)) {
            return jdbcTemplate.queryForList("SELECT * FROM warehouse WHERE status = ? ORDER BY warehouse_code", status.trim().toUpperCase());
        }
        return jdbcTemplate.queryForList("SELECT * FROM warehouse ORDER BY warehouse_code");
    }

    @Transactional
    public Map<String, Object> createWarehouse(StockDtos.WarehouseRequest request, String userId) {
        String id = uuid();
        jdbcTemplate.update("INSERT INTO warehouse (id, warehouse_code, name, description, status, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?)",
                id, required(request.getWarehouseCode(), "warehouseCode").trim().toUpperCase(), required(request.getName(), "name"), request.getDescription(), defaultStatus(request.getStatus()), nowTs(), userId, nowTs(), userId);
        audit("WAREHOUSE", id, "CREATE", null, null, userId, request.getWarehouseCode());
        return getWarehouse(id);
    }

    public Map<String, Object> getWarehouse(String id) {
        return jdbcTemplate.queryForMap("SELECT * FROM warehouse WHERE id = ?", id);
    }

    public List<Map<String, Object>> getStorageLocations(String warehouseId, String status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM storage_location WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (hasText(warehouseId)) { sql.append(" AND warehouse_id = ?"); args.add(warehouseId); }
        if (hasText(status)) { sql.append(" AND status = ?"); args.add(status.trim().toUpperCase()); }
        sql.append(" ORDER BY location_code");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public Map<String, Object> createStorageLocation(StockDtos.StorageLocationRequest request, String userId) {
        String id = uuid();
        jdbcTemplate.update("INSERT INTO storage_location (id, warehouse_id, location_code, name, location_type, status, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, required(request.getWarehouseId(), "warehouseId"), required(request.getLocationCode(), "locationCode").trim().toUpperCase(), required(request.getName(), "name"), defaultText(request.getLocationType(), "BIN"), defaultStatus(request.getStatus()), nowTs(), userId, nowTs(), userId);
        audit("STORAGE_LOCATION", id, "CREATE", null, null, userId, request.getLocationCode());
        return jdbcTemplate.queryForMap("SELECT * FROM storage_location WHERE id = ?", id);
    }

    public List<Map<String, Object>> getStock(String warehouseId, String storageLocationId, String productId, Boolean availableOnly) {
        StringBuilder sql = new StringBuilder("SELECT b.*, p.code AS product_code, p.description AS product_description, w.warehouse_code, w.name AS warehouse_name, l.location_code, l.name AS location_name FROM stock_balance b LEFT JOIN product p ON p.id = b.product_id LEFT JOIN warehouse w ON w.id = b.warehouse_id LEFT JOIN storage_location l ON l.id = b.storage_location_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (hasText(warehouseId)) { sql.append(" AND b.warehouse_id = ?"); args.add(warehouseId); }
        if (hasText(storageLocationId)) { sql.append(" AND b.storage_location_id = ?"); args.add(storageLocationId); }
        if (hasText(productId)) { sql.append(" AND b.product_id = ?"); args.add(productId); }
        if (Boolean.TRUE.equals(availableOnly)) { sql.append(" AND b.available_qty > 0"); }
        sql.append(" ORDER BY p.code, w.warehouse_code, l.location_code");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public List<Map<String, Object>> getMovements(String productId, String warehouseId, String storageLocationId, String movementType, String fromDate, String toDate) {
        StringBuilder sql = new StringBuilder("SELECT m.*, p.code AS product_code, p.description AS product_description, w.warehouse_code, fl.location_code AS from_location_code, tl.location_code AS to_location_code FROM stock_movement m LEFT JOIN product p ON p.id = m.product_id LEFT JOIN warehouse w ON w.id = m.warehouse_id LEFT JOIN storage_location fl ON fl.id = m.from_location_id LEFT JOIN storage_location tl ON tl.id = m.to_location_id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (hasText(productId)) { sql.append(" AND m.product_id = ?"); args.add(productId); }
        if (hasText(warehouseId)) { sql.append(" AND m.warehouse_id = ?"); args.add(warehouseId); }
        if (hasText(storageLocationId)) { sql.append(" AND (m.from_location_id = ? OR m.to_location_id = ?)"); args.add(storageLocationId); args.add(storageLocationId); }
        if (hasText(movementType)) { sql.append(" AND m.movement_type = ?"); args.add(movementType.trim().toUpperCase()); }
        if (hasText(fromDate)) { sql.append(" AND DATE(m.movement_at) >= ?"); args.add(Date.valueOf(fromDate)); }
        if (hasText(toDate)) { sql.append(" AND DATE(m.movement_at) <= ?"); args.add(Date.valueOf(toDate)); }
        sql.append(" ORDER BY m.movement_at DESC LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public Map<String, Object> createGoodsReceipt(StockDtos.GoodsReceiptRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (request.getLines() == null || request.getLines().isEmpty()) throw new IllegalArgumentException("At least one received product line is required");
        String receiptId = uuid();
        String receiptNo = nextNumber("GOODS_RECEIPT", "GRN");
        String warehouseId = required(request.getWarehouseId(), "warehouseId");
        String locationId = required(request.getStorageLocationId(), "storageLocationId");
        LocalDate receiptDate = request.getReceiptDate() == null ? LocalDate.now() : request.getReceiptDate();

        jdbcTemplate.update("INSERT INTO goods_receipt (id, receipt_no, supplier_partner_id, supplier_reference, warehouse_id, storage_location_id, receipt_date, status, notes, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                receiptId, receiptNo, request.getSupplierPartnerId(), request.getSupplierReference(), warehouseId, locationId, Date.valueOf(receiptDate), "RECEIVED", request.getNotes(), nowTs(), userId, nowTs(), userId);

        int lineNo = 10;
        for (StockDtos.GoodsReceiptLineRequest line : request.getLines()) {
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            String productId = resolveProductId(line.getProductId(), line.getProductCode());
            String lineId = uuid();
            jdbcTemplate.update("INSERT INTO goods_receipt_line (id, goods_receipt_id, line_no, product_id, quantity, open_putaway_qty, uom, batch_no, expiry_date, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    lineId, receiptId, lineNo, productId, qty, qty, defaultText(line.getUom(), "EA"), line.getBatchNo(), line.getExpiryDate() == null ? null : Date.valueOf(line.getExpiryDate()), nowTs(), userId);
            applyBalance(productId, warehouseId, locationId, qty, BigDecimal.ZERO, defaultText(line.getUom(), "EA"), line.getBatchNo(), userId);
            createMovement("GOODS_RECEIPT", receiptId, receiptNo, productId, warehouseId, null, locationId, qty, defaultText(line.getUom(), "EA"), line.getBatchNo(), userId, "Goods receipt " + receiptNo);
            lineNo += 10;
        }
        audit("GOODS_RECEIPT", receiptId, "CREATE", null, null, userId, receiptNo);
        return getGoodsReceipt(receiptId);
    }

    public List<Map<String, Object>> getGoodsReceipts(String status) {
        if (hasText(status)) return jdbcTemplate.queryForList("SELECT * FROM goods_receipt WHERE status = ? ORDER BY created_at DESC", status.trim().toUpperCase());
        return jdbcTemplate.queryForList("SELECT * FROM goods_receipt ORDER BY created_at DESC LIMIT 300");
    }

    public Map<String, Object> getGoodsReceipt(String id) {
        Map<String, Object> receipt = jdbcTemplate.queryForMap("SELECT * FROM goods_receipt WHERE id = ?", id);
        receipt.put("lines", jdbcTemplate.queryForList("SELECT l.*, p.code AS product_code, p.description AS product_description FROM goods_receipt_line l LEFT JOIN product p ON p.id = l.product_id WHERE l.goods_receipt_id = ? ORDER BY l.line_no", id));
        return receipt;
    }

    @Transactional
    public Map<String, Object> createPutaway(StockDtos.PutawayRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (request.getLines() == null || request.getLines().isEmpty()) throw new IllegalArgumentException("At least one putaway line is required");
        String putawayId = uuid();
        String putawayNo = nextNumber("PUTAWAY", "PUT");
        String warehouseId = required(request.getWarehouseId(), "warehouseId");
        String fromLocationId = required(request.getFromLocationId(), "fromLocationId");
        String toLocationId = required(request.getToLocationId(), "toLocationId");
        LocalDate movementDate = request.getMovementDate() == null ? LocalDate.now() : request.getMovementDate();
        jdbcTemplate.update("INSERT INTO putaway (id, putaway_no, goods_receipt_id, warehouse_id, from_location_id, to_location_id, movement_date, status, notes, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                putawayId, putawayNo, request.getGoodsReceiptId(), warehouseId, fromLocationId, toLocationId, Date.valueOf(movementDate), "COMPLETED", request.getNotes(), nowTs(), userId, nowTs(), userId);
        int lineNo = 10;
        for (StockDtos.PutawayLineRequest line : request.getLines()) {
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            String productId = required(line.getProductId(), "productId");
            String lineId = uuid();
            jdbcTemplate.update("INSERT INTO putaway_line (id, putaway_id, line_no, goods_receipt_line_id, product_id, quantity, uom, batch_no, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    lineId, putawayId, lineNo, line.getGoodsReceiptLineId(), productId, qty, defaultText(line.getUom(), "EA"), line.getBatchNo(), nowTs(), userId);
            applyBalance(productId, warehouseId, fromLocationId, qty.negate(), BigDecimal.ZERO, defaultText(line.getUom(), "EA"), line.getBatchNo(), userId);
            applyBalance(productId, warehouseId, toLocationId, qty, BigDecimal.ZERO, defaultText(line.getUom(), "EA"), line.getBatchNo(), userId);
            if (hasText(line.getGoodsReceiptLineId())) {
                jdbcTemplate.update("UPDATE goods_receipt_line SET open_putaway_qty = GREATEST(open_putaway_qty - ?, 0) WHERE id = ?", qty, line.getGoodsReceiptLineId());
            }
            createMovement("PUTAWAY", putawayId, putawayNo, productId, warehouseId, fromLocationId, toLocationId, qty, defaultText(line.getUom(), "EA"), line.getBatchNo(), userId, "Putaway " + putawayNo);
            lineNo += 10;
        }
        audit("PUTAWAY", putawayId, "CREATE", null, null, userId, putawayNo);
        return getPutaway(putawayId);
    }

    public List<Map<String, Object>> getPutaways() {
        return jdbcTemplate.queryForList("SELECT * FROM putaway ORDER BY created_at DESC LIMIT 300");
    }

    public Map<String, Object> getPutaway(String id) {
        Map<String, Object> putaway = jdbcTemplate.queryForMap("SELECT * FROM putaway WHERE id = ?", id);
        putaway.put("lines", jdbcTemplate.queryForList("SELECT l.*, p.code AS product_code, p.description AS product_description FROM putaway_line l LEFT JOIN product p ON p.id = l.product_id WHERE l.putaway_id = ? ORDER BY l.line_no", id));
        return putaway;
    }

    @Transactional
    public Map<String, Object> createSalesOrder(StockDtos.SalesOrderRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (request.getLines() == null || request.getLines().isEmpty()) throw new IllegalArgumentException("At least one order line is required");
        String salesOrderId = uuid();
        String salesOrderNo = nextNumber("SALES_ORDER", "SO");
        LocalDate orderDate = request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate();
        jdbcTemplate.update("INSERT INTO sales_order (id, sales_order_no, customer_partner_id, order_date, requested_delivery_date, warehouse_id, status, notes, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                salesOrderId, salesOrderNo, request.getCustomerPartnerId(), Date.valueOf(orderDate), parseDateOrNull(request.getRequestedDeliveryDate()), request.getWarehouseId(), "OPEN", request.getNotes(), nowTs(), userId, nowTs(), userId);
        int lineNo = 10;
        for (StockDtos.SalesOrderLineRequest line : request.getLines()) {
            String productId = resolveProductId(line.getProductId(), line.getProductCode());
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            BigDecimal available = availableStock(productId, request.getWarehouseId());
            BigDecimal allocated = available.min(qty);
            String status = allocated.compareTo(qty) >= 0 ? "AVAILABLE" : (allocated.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "BACKORDER");
            jdbcTemplate.update("INSERT INTO sales_order_line (id, sales_order_id, line_no, product_id, quantity, allocated_qty, issued_qty, uom, status, notes, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    uuid(), salesOrderId, lineNo, productId, qty, allocated, BigDecimal.ZERO, defaultText(line.getUom(), "EA"), status, line.getNotes(), nowTs(), userId);
            lineNo += 10;
        }
        audit("SALES_ORDER", salesOrderId, "CREATE", null, null, userId, salesOrderNo);
        return getSalesOrder(salesOrderId);
    }

    public List<Map<String, Object>> getSalesOrders(String status) {
        if (hasText(status)) return jdbcTemplate.queryForList("SELECT * FROM sales_order WHERE status = ? ORDER BY created_at DESC", status.trim().toUpperCase());
        return jdbcTemplate.queryForList("SELECT * FROM sales_order ORDER BY created_at DESC LIMIT 300");
    }

    public Map<String, Object> getSalesOrder(String id) {
        Map<String, Object> order = jdbcTemplate.queryForMap("SELECT * FROM sales_order WHERE id = ?", id);
        order.put("lines", jdbcTemplate.queryForList("SELECT l.*, p.code AS product_code, p.description AS product_description FROM sales_order_line l LEFT JOIN product p ON p.id = l.product_id WHERE l.sales_order_id = ? ORDER BY l.line_no", id));
        return order;
    }

    @Transactional
    public Map<String, Object> updateSalesOrderStatus(String id, StockDtos.StatusUpdateRequest request, String userId) {
        String status = required(request.getStatus(), "status").trim().toUpperCase();
        jdbcTemplate.update("UPDATE sales_order SET status = ?, updated_at = ?, updated_by = ? WHERE id = ?", status, nowTs(), userId, id);
        audit("SALES_ORDER", id, "STATUS", null, status, userId, request.getNotes());
        return getSalesOrder(id);
    }

    public StockDtos.StockDashboardResponse dashboard() {
        StockDtos.StockDashboardResponse response = new StockDtos.StockDashboardResponse();
        response.setTotalStockQuantity(queryBigDecimal("SELECT COALESCE(SUM(on_hand_qty),0) FROM stock_balance"));
        response.setProductCount(queryInt("SELECT COUNT(DISTINCT product_id) FROM stock_balance WHERE on_hand_qty <> 0"));
        response.setLowStockCount(queryInt("SELECT COUNT(*) FROM stock_balance WHERE on_hand_qty <= minimum_qty"));
        response.setGoodsReceiptsToday(queryInt("SELECT COUNT(*) FROM goods_receipt WHERE DATE(created_at) = CURRENT_DATE"));
        response.setStockMovementsToday(queryInt("SELECT COUNT(*) FROM stock_movement WHERE DATE(movement_at) = CURRENT_DATE"));
        response.setOpenSalesOrders(queryInt("SELECT COUNT(*) FROM sales_order WHERE status IN ('OPEN','PARTIAL')"));
        response.setActiveWarehouses(queryInt("SELECT COUNT(*) FROM warehouse WHERE status = 'ACTIVE'"));
        response.setStockByWarehouse(jdbcTemplate.queryForList("SELECT w.id AS warehouse_id, w.warehouse_code, w.name, COALESCE(SUM(b.on_hand_qty),0) AS on_hand_qty FROM warehouse w LEFT JOIN stock_balance b ON b.warehouse_id = w.id GROUP BY w.id, w.warehouse_code, w.name ORDER BY w.warehouse_code"));
        response.setLowStock(jdbcTemplate.queryForList("SELECT b.*, p.code AS product_code, p.description AS product_description, w.warehouse_code, l.location_code FROM stock_balance b LEFT JOIN product p ON p.id = b.product_id LEFT JOIN warehouse w ON w.id = b.warehouse_id LEFT JOIN storage_location l ON l.id = b.storage_location_id WHERE b.on_hand_qty <= b.minimum_qty ORDER BY p.code LIMIT 50"));
        response.setRecentMovements(jdbcTemplate.queryForList("SELECT m.*, p.code AS product_code, p.description AS product_description FROM stock_movement m LEFT JOIN product p ON p.id = m.product_id ORDER BY m.movement_at DESC LIMIT 20"));
        response.setUserActivity(jdbcTemplate.queryForList("SELECT created_by, action, entity_type, COUNT(*) AS activity_count, MAX(created_at) AS last_activity_at FROM stock_audit_log GROUP BY created_by, action, entity_type ORDER BY last_activity_at DESC LIMIT 20"));
        return response;
    }

    public List<Map<String, Object>> auditTrail(String entityType, String entityId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM stock_audit_log WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (hasText(entityType)) { sql.append(" AND entity_type = ?"); args.add(entityType.trim().toUpperCase()); }
        if (hasText(entityId)) { sql.append(" AND entity_id = ?"); args.add(entityId); }
        sql.append(" ORDER BY created_at DESC LIMIT 500");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    private String resolveProductId(String productId, String productCode) {
        if (hasText(productId)) return productId.trim();
        if (!hasText(productCode)) throw new IllegalArgumentException("productId or productCode is required");
        List<String> ids = jdbcTemplate.queryForList("SELECT id FROM product WHERE code = ?", String.class, productCode.trim());
        if (ids.isEmpty()) throw new IllegalArgumentException("Product not found for code: " + productCode);
        return ids.get(0);
    }

    private void applyBalance(String productId, String warehouseId, String locationId, BigDecimal qtyDelta, BigDecimal reservedDelta, String uom, String batchNo, String userId) {
        List<String> ids = jdbcTemplate.queryForList("SELECT id FROM stock_balance WHERE product_id = ? AND warehouse_id = ? AND storage_location_id = ? AND COALESCE(batch_no,'') = COALESCE(?, '')", String.class, productId, warehouseId, locationId, batchNo);
        if (ids.isEmpty()) {
            jdbcTemplate.update("INSERT INTO stock_balance (id, product_id, warehouse_id, storage_location_id, batch_no, on_hand_qty, reserved_qty, available_qty, uom, minimum_qty, last_movement_at, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    uuid(), productId, warehouseId, locationId, batchNo, qtyDelta, reservedDelta, qtyDelta.subtract(reservedDelta), uom, BigDecimal.ZERO, nowTs(), nowTs(), userId);
        } else {
            jdbcTemplate.update("UPDATE stock_balance SET on_hand_qty = on_hand_qty + ?, reserved_qty = reserved_qty + ?, available_qty = on_hand_qty + ? - (reserved_qty + ?), last_movement_at = ?, updated_at = ?, updated_by = ? WHERE id = ?",
                    qtyDelta, reservedDelta, qtyDelta, reservedDelta, nowTs(), nowTs(), userId, ids.get(0));
        }
    }

    private void createMovement(String movementType, String referenceId, String referenceNo, String productId, String warehouseId, String fromLocationId, String toLocationId, BigDecimal quantity, String uom, String batchNo, String userId, String notes) {
        String movementNo = nextNumber("STOCK_MOVEMENT", "STM");
        jdbcTemplate.update("INSERT INTO stock_movement (id, movement_no, movement_type, reference_type, reference_id, reference_no, product_id, warehouse_id, from_location_id, to_location_id, quantity, uom, batch_no, movement_at, processed_by, notes, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                uuid(), movementNo, movementType, movementType, referenceId, referenceNo, productId, warehouseId, fromLocationId, toLocationId, quantity, uom, batchNo, Timestamp.valueOf(LocalDateTime.now()), userId, notes, nowTs(), userId);
        audit("STOCK_MOVEMENT", referenceId, movementType, null, quantity.toPlainString(), userId, referenceNo);
    }

    private BigDecimal availableStock(String productId, String warehouseId) {
        if (hasText(warehouseId)) return queryBigDecimal("SELECT COALESCE(SUM(available_qty),0) FROM stock_balance WHERE product_id = ? AND warehouse_id = ?", productId, warehouseId);
        return queryBigDecimal("SELECT COALESCE(SUM(available_qty),0) FROM stock_balance WHERE product_id = ?", productId);
    }

    private void audit(String entityType, String entityId, String action, String oldValue, String newValue, String userId, String notes) {
        jdbcTemplate.update("INSERT INTO stock_audit_log (id, entity_type, entity_id, action, old_value, new_value, notes, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?)",
                uuid(), entityType, entityId, action, oldValue, newValue, notes, nowTs(), userId);
    }

    private String nextNumber(String object, String fallbackPrefix) {
        try {
            String number = numberRangeService.generateNumber(object);
            if (hasText(number)) return number;
        } catch (NumberRangeObjectNotFound ignored) {
        } catch (Exception ignored) {
        }
        return fallbackPrefix + "-" + System.currentTimeMillis();
    }

    private Integer queryInt(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private BigDecimal queryBigDecimal(String sql, Object... args) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private Date parseDateOrNull(String value) {
        if (!hasText(value)) return null;
        return Date.valueOf(value.trim());
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(field + " must be greater than zero");
        return value;
    }

    private String required(String value, String field) {
        if (!hasText(value)) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private String defaultStatus(String status) { return hasText(status) ? status.trim().toUpperCase() : "ACTIVE"; }
    private String defaultText(String value, String defaultValue) { return hasText(value) ? value.trim() : defaultValue; }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String uuid() { return UUID.randomUUID().toString().replace("-", ""); }
    private Timestamp nowTs() { return Timestamp.valueOf(LocalDateTime.now()); }
}
