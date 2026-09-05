package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.InvoiceOutboundDto;
import za.co.mawa.bes.dto.v2.stock.StockDtos;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.enums.ProductTypeCode;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.service.InvoiceService;
import za.co.mawa.bes.service.NumberRangeService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StockOperationsService {
    private final JdbcTemplate jdbcTemplate;
    private final NumberRangeService numberRangeService;
    private final InvoiceService invoiceService;

    public StockOperationsService(JdbcTemplate jdbcTemplate, NumberRangeService numberRangeService, InvoiceService invoiceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.numberRangeService = numberRangeService;
        this.invoiceService = invoiceService;
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
        StringBuilder sql = new StringBuilder("SELECT l.*, w.warehouse_code, t.name AS location_type_name, t.purpose AS location_type_purpose, t.available_for_sale, t.available_for_issue, t.allow_putaway, t.allow_picking, t.allow_reservation, t.allow_negative_stock, t.requires_batch, t.requires_expiry_date, t.requires_serial_number, t.requires_quality_release, t.restricted_access, t.temporary_location, t.system_managed FROM storage_location l LEFT JOIN warehouse w ON w.id = l.warehouse_id LEFT JOIN storage_location_type t ON t.code = l.location_type WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (hasText(warehouseId)) { sql.append(" AND l.warehouse_id = ?"); args.add(warehouseId); }
        if (hasText(status)) { sql.append(" AND l.status = ?"); args.add(status.trim().toUpperCase()); }
        sql.append(" ORDER BY w.warehouse_code, l.location_code");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    private void resolveAuditActors(List<Map<String, Object>> rows, String field) {
        for (Map<String, Object> row : rows) {
            Object actor = row.get(field);
            if (actor == null) continue;
            List<String> usernames = jdbcTemplate.query(
                    "SELECT username FROM user WHERE id=? OR username=? LIMIT 1",
                    (rs, index) -> rs.getString(1), actor.toString(), actor.toString());
            if (!usernames.isEmpty()) row.put(field, usernames.get(0));
        }
    }

    @Transactional
    public Map<String, Object> createStorageLocation(StockDtos.StorageLocationRequest request, String userId) {
        String id = uuid();
        String warehouseId = required(request.getWarehouseId(), "warehouseId");
        String locationType = requireLocationType(defaultText(request.getLocationType(), "GENERAL_STORAGE"));
        jdbcTemplate.update("INSERT INTO storage_location (id, warehouse_id, location_code, name, location_type, status, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?)",
                id, warehouseId, required(request.getLocationCode(), "locationCode").trim().toUpperCase(), required(request.getName(), "name"), locationType, defaultStatus(request.getStatus()), nowTs(), userId, nowTs(), userId);
        audit("STORAGE_LOCATION", id, "CREATE", null, null, userId, request.getLocationCode());
        return jdbcTemplate.queryForMap("SELECT * FROM storage_location WHERE id = ?", id);
    }

    public List<Map<String, Object>> getStock(String warehouseId, String storageLocationId, String productId, Boolean availableOnly) {
        StringBuilder sql = new StringBuilder("SELECT b.*, p.code AS product_code, p.description AS product_description, w.warehouse_code, w.name AS warehouse_name, l.location_code, l.name AS location_name, l.location_type, t.name AS location_type_name, t.available_for_sale, t.available_for_issue, t.allow_picking, t.allow_reservation FROM stock_balance b LEFT JOIN product p ON p.id = b.product_id LEFT JOIN warehouse w ON w.id = b.warehouse_id LEFT JOIN storage_location l ON l.id = b.storage_location_id LEFT JOIN storage_location_type t ON t.code = l.location_type WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (hasText(warehouseId)) { sql.append(" AND b.warehouse_id = ?"); args.add(warehouseId); }
        if (hasText(storageLocationId)) { sql.append(" AND b.storage_location_id = ?"); args.add(storageLocationId); }
        if (hasText(productId)) { sql.append(" AND b.product_id = ?"); args.add(productId); }
        if (Boolean.TRUE.equals(availableOnly)) { sql.append(" AND b.available_qty > 0 AND COALESCE(t.available_for_issue,0)=1"); }
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

    // ---------------------------------------------------------------------
    // Quotations
    // ---------------------------------------------------------------------
    @Transactional
    public Map<String, Object> createQuotation(StockDtos.QuotationRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (request.getLines() == null || request.getLines().isEmpty()) throw new IllegalArgumentException("At least one quotation line is required");
        String id = uuid();
        String quotationNo = nextNumber("QUOTATION", "QT");
        LocalDate quotationDate = request.getQuotationDate() == null ? LocalDate.now() : request.getQuotationDate();
        AmountTotals totals = totals(request.getLines());
        jdbcTemplate.update("INSERT INTO quotation (id, quotation_no, customer_partner_id, customer_reference, quotation_date, valid_until, requested_delivery_date, status, currency, subtotal_amount, tax_amount, total_amount, notes, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, quotationNo, request.getCustomerPartnerId(), request.getCustomerReference(), Date.valueOf(quotationDate), toSqlDate(request.getValidUntil()), toSqlDate(request.getRequestedDeliveryDate()), defaultText(request.getStatus(), "DRAFT").toUpperCase(), defaultText(request.getCurrency(), "ZAR"), totals.subtotal, totals.tax, totals.total, request.getNotes(), nowTs(), userId, nowTs(), userId);
        int lineNo = 10;
        for (StockDtos.CommercialLineRequest line : request.getLines()) {
            String productId = resolveProductId(line.getProductId(), line.getProductCode());
            requireSaleable(productId);
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            BigDecimal unitPrice = money(line.getUnitPrice());
            BigDecimal taxRate = percent(line.getTaxRate());
            BigDecimal lineSubtotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            jdbcTemplate.update("INSERT INTO quotation_line (id, quotation_id, line_no, product_id, product_description, quantity, uom, unit_price, tax_rate, line_subtotal, line_tax, line_total, notes, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    uuid(), id, lineNo, productId, line.getDescription(), qty, defaultText(line.getUom(), "EA"), unitPrice, taxRate, lineSubtotal, lineTax, lineSubtotal.add(lineTax), line.getNotes(), nowTs(), userId);
            lineNo += 10;
        }
        audit("QUOTATION", id, "CREATE", null, null, userId, quotationNo);
        return getQuotation(id);
    }

    public List<Map<String, Object>> getQuotations(String status, String customerPartnerId) {
        StringBuilder sql = new StringBuilder("SELECT q.*, p.number AS customer_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS customer_name, COALESCE(l.line_count,0) AS line_count, " +
                "(SELECT i.id FROM invoice i WHERE i.source_type='QUOTATION' AND i.source_id=q.id ORDER BY i.invoice_date DESC, i.invoice_no DESC LIMIT 1) AS invoice_id, " +
                "(SELECT i.invoice_no FROM invoice i WHERE i.source_type='QUOTATION' AND i.source_id=q.id ORDER BY i.invoice_date DESC, i.invoice_no DESC LIMIT 1) AS invoice_no " +
                "FROM quotation q LEFT JOIN partner p ON p.id = q.customer_partner_id LEFT JOIN (SELECT quotation_id, COUNT(*) AS line_count FROM quotation_line GROUP BY quotation_id) l ON l.quotation_id = q.id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (hasText(status)) { sql.append(" AND q.status = ?"); args.add(status.trim().toUpperCase()); }
        if (hasText(customerPartnerId)) { sql.append(" AND q.customer_partner_id = ?"); args.add(customerPartnerId); }
        sql.append(" ORDER BY q.created_at DESC LIMIT 300");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> getQuotation(String id) {
        Map<String, Object> quotation = jdbcTemplate.queryForMap("SELECT q.*, p.number AS customer_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS customer_name, " +
                "(SELECT i.id FROM invoice i WHERE i.source_type='QUOTATION' AND i.source_id=q.id ORDER BY i.invoice_date DESC, i.invoice_no DESC LIMIT 1) AS invoice_id, " +
                "(SELECT i.invoice_no FROM invoice i WHERE i.source_type='QUOTATION' AND i.source_id=q.id ORDER BY i.invoice_date DESC, i.invoice_no DESC LIMIT 1) AS invoice_no " +
                "FROM quotation q LEFT JOIN partner p ON p.id = q.customer_partner_id WHERE q.id = ?", id);
        quotation.put("lines", jdbcTemplate.queryForList("SELECT l.*, p.code AS product_code, p.description AS product_description FROM quotation_line l LEFT JOIN product p ON p.id = l.product_id WHERE l.quotation_id = ? ORDER BY l.line_no", id));
        return quotation;
    }

    @Transactional
    public Map<String, Object> updateQuotation(String id, StockDtos.QuotationRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        Map<String, Object> current = getQuotation(id);
        String currentStatus = text(current.get("status")).trim().toUpperCase(Locale.ROOT);
        if ("INVOICED".equals(currentStatus) || "CONVERTED".equals(currentStatus)) {
            throw new IllegalStateException("This quotation has already been converted and can no longer be edited");
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one quotation line is required");
        }
        AmountTotals totals = totals(request.getLines());
        LocalDate quotationDate = request.getQuotationDate() == null
                ? dateValue(current.get("quotation_date"), LocalDate.now())
                : request.getQuotationDate();
        String status = hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase(Locale.ROOT) : currentStatus;
        jdbcTemplate.update("UPDATE quotation SET customer_partner_id=?, customer_reference=?, quotation_date=?, valid_until=?, requested_delivery_date=?, status=?, currency=?, subtotal_amount=?, tax_amount=?, total_amount=?, notes=?, updated_at=?, updated_by=? WHERE id=?",
                request.getCustomerPartnerId(), request.getCustomerReference(), Date.valueOf(quotationDate), toSqlDate(request.getValidUntil()),
                toSqlDate(request.getRequestedDeliveryDate()), status, defaultText(request.getCurrency(), defaultText(text(current.get("currency")), "ZAR")),
                totals.subtotal, totals.tax, totals.total, request.getNotes(), nowTs(), userId, id);

        jdbcTemplate.update("DELETE FROM quotation_line WHERE quotation_id = ?", id);
        int lineNo = 10;
        for (StockDtos.CommercialLineRequest line : request.getLines()) {
            String productId = resolveProductId(line.getProductId(), line.getProductCode());
            requireSaleable(productId);
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            BigDecimal unitPrice = money(line.getUnitPrice());
            BigDecimal taxRate = percent(line.getTaxRate());
            BigDecimal lineSubtotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            jdbcTemplate.update("INSERT INTO quotation_line (id, quotation_id, line_no, product_id, product_description, quantity, uom, unit_price, tax_rate, line_subtotal, line_tax, line_total, notes, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    uuid(), id, lineNo, productId, line.getDescription(), qty, defaultText(line.getUom(), "EA"), unitPrice, taxRate, lineSubtotal, lineTax, lineSubtotal.add(lineTax), line.getNotes(), nowTs(), userId);
            lineNo += 10;
        }
        audit("QUOTATION", id, "UPDATE", null, null, userId, text(current.get("quotation_no")));
        return getQuotation(id);
    }

    @Transactional
    public InvoiceOutboundDto convertQuotationToInvoice(String quotationId, String userId) {
        jdbcTemplate.queryForMap("SELECT id FROM quotation WHERE id = ? FOR UPDATE", quotationId);
        Map<String, Object> quotation = getQuotation(quotationId);
        String existingInvoiceId = text(quotation.get("invoice_id"));
        if (hasText(existingInvoiceId)) {
            jdbcTemplate.update("UPDATE quotation SET status='INVOICED', updated_at=?, updated_by=? WHERE id=?", nowTs(), userId, quotationId);
            return invoiceService.getInvoiceDto(existingInvoiceId)
                    .orElseThrow(() -> new IllegalStateException("The quotation is linked to an invoice that could not be loaded"));
        }
        String customerPartnerId = required(text(quotation.get("customer_partner_id")), "customerPartnerId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) quotation.get("lines");
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("The quotation has no line items");

        long subtotalCents = cents(decimal(quotation.get("subtotal_amount")));
        long taxCents = cents(decimal(quotation.get("tax_amount")));
        long totalCents = cents(decimal(quotation.get("total_amount")));
        String quotationNo = text(quotation.get("quotation_no"));
        String customerReference = text(quotation.get("customer_reference"));
        String quotationNotes = text(quotation.get("notes"));

        InvoiceEntity invoice = InvoiceEntity.builder()
                .externalRef(hasText(customerReference) ? customerReference : quotationNo)
                .sourceType("QUOTATION")
                .sourceId(quotationId)
                .partnerId(customerPartnerId)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status("DRAFT")
                .subtotalCents(subtotalCents)
                .taxCents(taxCents)
                .discountCents(0L)
                .totalCents(totalCents)
                .paidCents(0L)
                .creditedCents(0L)
                .balanceCents(totalCents)
                .currency(defaultText(text(quotation.get("currency")), "ZAR"))
                .notes(hasText(quotationNotes) ? quotationNotes + "\nCreated from quotation " + quotationNo : "Created from quotation " + quotationNo)
                .createdBy(userId)
                .updatedBy(userId)
                .updatedAt(LocalDateTime.now())
                .lines(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        for (Map<String, Object> line : lines) {
            InvoiceLineEntity invoiceLine = InvoiceLineEntity.builder()
                    .productId(text(line.get("product_id")))
                    .description(defaultText(text(line.get("product_description")), text(line.get("product_code"))))
                    .quantity(decimal(line.get("quantity")).doubleValue())
                    .showAmount(true)
                    .unitPriceCents(cents(decimal(line.get("unit_price"))))
                    .discountCents(0L)
                    .taxCents(cents(decimal(line.get("line_tax"))))
                    .subtotalCents(cents(decimal(line.get("line_subtotal"))))
                    .totalCents(cents(decimal(line.get("line_total"))))
                    .build();
            invoice.getLines().add(invoiceLine);
        }

        InvoiceEntity created = invoiceService.createInvoice(invoice);
        jdbcTemplate.update("UPDATE quotation SET status='INVOICED', updated_at=?, updated_by=? WHERE id=?", nowTs(), userId, quotationId);
        audit("QUOTATION", quotationId, "CREATE_INVOICE", null, created.getInvoiceNo(), userId, created.getId());
        return invoiceService.mapToDto(created);
    }

    @Transactional
    public Map<String, Object> updateQuotationStatus(String id, StockDtos.StatusUpdateRequest request, String userId) {
        String status = required(request.getStatus(), "status").trim().toUpperCase();
        String currentStatus = text(getQuotation(id).get("status")).trim().toUpperCase(Locale.ROOT);
        if (("INVOICED".equals(currentStatus) || "CONVERTED".equals(currentStatus)) && !currentStatus.equals(status)) {
            throw new IllegalStateException("A converted quotation is read-only");
        }
        jdbcTemplate.update("UPDATE quotation SET status = ?, updated_at = ?, updated_by = ? WHERE id = ?", status, nowTs(), userId, id);
        audit("QUOTATION", id, "STATUS", null, status, userId, request.getNotes());
        return getQuotation(id);
    }

    @Transactional
    public Map<String, Object> convertQuotationToSalesOrder(String quotationId, StockDtos.ConvertQuotationRequest request, String userId) {
        Map<String, Object> quotation = getQuotation(quotationId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) quotation.get("lines");
        StockDtos.SalesOrderRequest salesRequest = new StockDtos.SalesOrderRequest();
        salesRequest.setQuotationId(quotationId);
        salesRequest.setCustomerPartnerId(text(quotation.get("customer_partner_id")));
        salesRequest.setCustomerReference(text(quotation.get("customer_reference")));
        salesRequest.setRequestedDeliveryDate(request == null || request.getRequestedDeliveryDate() == null ? textDate(quotation.get("requested_delivery_date")) : request.getRequestedDeliveryDate().toString());
        salesRequest.setWarehouseId(request == null ? null : request.getWarehouseId());
        salesRequest.setCurrency(defaultText(text(quotation.get("currency")), "ZAR"));
        salesRequest.setNotes(request == null || !hasText(request.getNotes()) ? "Created from quotation " + quotation.get("quotation_no") : request.getNotes());
        for (Map<String, Object> line : lines) {
            StockDtos.SalesOrderLineRequest soLine = new StockDtos.SalesOrderLineRequest();
            soLine.setProductId(text(line.get("product_id")));
            soLine.setDescription(text(line.get("product_description")));
            soLine.setQuantity(decimal(line.get("quantity")));
            soLine.setUom(defaultText(text(line.get("uom")), "EA"));
            soLine.setUnitPrice(decimal(line.get("unit_price")));
            soLine.setTaxRate(decimal(line.get("tax_rate")));
            salesRequest.getLines().add(soLine);
        }
        Map<String, Object> salesOrder = createSalesOrder(salesRequest, userId);
        jdbcTemplate.update("UPDATE quotation SET status = 'CONVERTED', converted_sales_order_id = ?, updated_at = ?, updated_by = ? WHERE id = ?", salesOrder.get("id"), nowTs(), userId, quotationId);
        audit("QUOTATION", quotationId, "CONVERT_TO_SALES_ORDER", null, text(salesOrder.get("sales_order_no")), userId, null);
        return salesOrder;
    }

    // ---------------------------------------------------------------------
    // Purchase Orders
    // ---------------------------------------------------------------------
    @Transactional
    public Map<String, Object> createPurchaseOrder(StockDtos.PurchaseOrderRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (request.getLines() == null || request.getLines().isEmpty()) throw new IllegalArgumentException("At least one purchase order line is required");
        String id = uuid();
        String purchaseOrderNo = nextNumber("PURCHASE_ORDER", "PO");
        LocalDate orderDate = request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate();
        AmountTotals totals = totals(request.getLines());
        jdbcTemplate.update("INSERT INTO purchase_order (id, purchase_order_no, supplier_partner_id, supplier_reference, order_date, expected_delivery_date, warehouse_id, receiving_location_id, status, currency, subtotal_amount, tax_amount, total_amount, notes, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, purchaseOrderNo, request.getSupplierPartnerId(), request.getSupplierReference(), Date.valueOf(orderDate), toSqlDate(request.getExpectedDeliveryDate()), request.getWarehouseId(), request.getReceivingLocationId(), defaultText(request.getStatus(), "DRAFT").toUpperCase(), defaultText(request.getCurrency(), "ZAR"), totals.subtotal, totals.tax, totals.total, request.getNotes(), nowTs(), userId, nowTs(), userId);
        int lineNo = 10;
        for (StockDtos.CommercialLineRequest line : request.getLines()) {
            String productId = resolveProductId(line.getProductId(), line.getProductCode());
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            BigDecimal unitPrice = money(line.getUnitPrice());
            BigDecimal taxRate = percent(line.getTaxRate());
            BigDecimal lineSubtotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            jdbcTemplate.update("INSERT INTO purchase_order_line (id, purchase_order_id, line_no, product_id, product_description, ordered_qty, received_qty, open_qty, uom, unit_cost, tax_rate, line_subtotal, line_tax, line_total, status, notes, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    uuid(), id, lineNo, productId, line.getDescription(), qty, BigDecimal.ZERO, qty, defaultText(line.getUom(), "EA"), unitPrice, taxRate, lineSubtotal, lineTax, lineSubtotal.add(lineTax), "OPEN", line.getNotes(), nowTs(), userId);
            lineNo += 10;
        }
        audit("PURCHASE_ORDER", id, "CREATE", null, null, userId, purchaseOrderNo);
        return getPurchaseOrder(id);
    }

    public List<Map<String, Object>> getPurchaseOrders(String status, String supplierPartnerId) {
        StringBuilder sql = new StringBuilder("SELECT po.*, p.number AS supplier_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS supplier_name, COALESCE(l.line_count,0) AS line_count FROM purchase_order po LEFT JOIN partner p ON p.id = po.supplier_partner_id LEFT JOIN (SELECT purchase_order_id, COUNT(*) AS line_count FROM purchase_order_line GROUP BY purchase_order_id) l ON l.purchase_order_id = po.id WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (hasText(status)) { sql.append(" AND po.status = ?"); args.add(status.trim().toUpperCase()); }
        if (hasText(supplierPartnerId)) { sql.append(" AND po.supplier_partner_id = ?"); args.add(supplierPartnerId); }
        sql.append(" ORDER BY po.created_at DESC LIMIT 300");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public Map<String, Object> getPurchaseOrder(String id) {
        Map<String, Object> po = jdbcTemplate.queryForMap("SELECT po.*, p.number AS supplier_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS supplier_name FROM purchase_order po LEFT JOIN partner p ON p.id = po.supplier_partner_id WHERE po.id = ?", id);
        po.put("lines", jdbcTemplate.queryForList("SELECT l.*, p.code AS product_code, p.description AS product_description FROM purchase_order_line l LEFT JOIN product p ON p.id = l.product_id WHERE l.purchase_order_id = ? ORDER BY l.line_no", id));
        return po;
    }

    @Transactional
    public Map<String, Object> updatePurchaseOrderStatus(String id, StockDtos.StatusUpdateRequest request, String userId) {
        String status = required(request.getStatus(), "status").trim().toUpperCase();
        jdbcTemplate.update("UPDATE purchase_order SET status = ?, updated_at = ?, updated_by = ? WHERE id = ?", status, nowTs(), userId, id);
        audit("PURCHASE_ORDER", id, "STATUS", null, status, userId, request.getNotes());
        return getPurchaseOrder(id);
    }

    @Transactional
    public Map<String, Object> createGoodsReceiptForPurchaseOrder(String purchaseOrderId, StockDtos.GoodsReceiptRequest request, String userId) {
        Map<String, Object> po = getPurchaseOrder(purchaseOrderId);
        if (request == null) request = new StockDtos.GoodsReceiptRequest();
        request.setPurchaseOrderId(purchaseOrderId);
        request.setPurchaseOrderNo(text(po.get("purchase_order_no")));
        request.setSupplierPartnerId(defaultText(request.getSupplierPartnerId(), text(po.get("supplier_partner_id"))));
        request.setSupplierReference(defaultText(request.getSupplierReference(), text(po.get("supplier_reference"))));
        request.setWarehouseId(defaultText(request.getWarehouseId(), text(po.get("warehouse_id"))));
        request.setStorageLocationId(defaultText(request.getStorageLocationId(), text(po.get("receiving_location_id"))));
        if (request.getLines() == null || request.getLines().isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> poLines = (List<Map<String, Object>>) po.get("lines");
            for (Map<String, Object> poLine : poLines) {
                BigDecimal openQty = decimal(poLine.get("open_qty"));
                if (openQty.compareTo(BigDecimal.ZERO) > 0) {
                    StockDtos.GoodsReceiptLineRequest grLine = new StockDtos.GoodsReceiptLineRequest();
                    grLine.setPurchaseOrderLineId(text(poLine.get("id")));
                    grLine.setProductId(text(poLine.get("product_id")));
                    grLine.setQuantity(openQty);
                    grLine.setUom(defaultText(text(poLine.get("uom")), "EA"));
                    grLine.setUnitCost(decimal(poLine.get("unit_cost")));
                    grLine.setTaxRate(decimal(poLine.get("tax_rate")));
                    request.getLines().add(grLine);
                }
            }
        }
        return createGoodsReceipt(request, userId);
    }

    // ---------------------------------------------------------------------
    // Goods Receipt and Putaway
    // ---------------------------------------------------------------------
    @Transactional
    public Map<String, Object> createGoodsReceipt(StockDtos.GoodsReceiptRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (request.getLines() == null || request.getLines().isEmpty()) throw new IllegalArgumentException("At least one received product line is required");
        String receiptId = uuid();
        String receiptNo = nextNumber("GOODS_RECEIPT", "GRN");
        String warehouseId = required(request.getWarehouseId(), "warehouseId");
        String locationId = required(request.getStorageLocationId(), "storageLocationId");
        LocationProfile receiptLocation = requireActiveLocation(warehouseId, locationId);
        LocalDate receiptDate = request.getReceiptDate() == null ? LocalDate.now() : request.getReceiptDate();
        AmountTotals totals = goodsReceiptTotals(request.getLines());

        jdbcTemplate.update("INSERT INTO goods_receipt (id, receipt_no, purchase_order_id, purchase_order_no, supplier_partner_id, supplier_reference, warehouse_id, storage_location_id, receipt_date, status, currency, subtotal_amount, tax_amount, total_amount, notes, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                receiptId, receiptNo, request.getPurchaseOrderId(), request.getPurchaseOrderNo(), request.getSupplierPartnerId(), request.getSupplierReference(), warehouseId, locationId, Date.valueOf(receiptDate), "RECEIVED", "ZAR", totals.subtotal, totals.tax, totals.total, request.getNotes(), nowTs(), userId, nowTs(), userId);

        int lineNo = 10;
        for (StockDtos.GoodsReceiptLineRequest line : request.getLines()) {
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            String productId = resolveProductId(line.getProductId(), line.getProductCode());
            ProductProfile product = requireReceivable(productId);
            if (receiptLocation.requiresBatch() && !hasText(line.getBatchNo())) {
                throw new IllegalArgumentException("Batch number is required for storage location " + receiptLocation.code());
            }
            if (receiptLocation.requiresExpiryDate() && line.getExpiryDate() == null) {
                throw new IllegalArgumentException("Expiry date is required for storage location " + receiptLocation.code());
            }
            String lineId = uuid();
            BigDecimal unitCost = money(line.getUnitCost());
            BigDecimal taxRate = percent(line.getTaxRate());
            BigDecimal lineSubtotal = unitCost.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineSubtotal.add(lineTax).setScale(2, RoundingMode.HALF_UP);
            BigDecimal openPutawayQty = product.type().isCanBePutAway() ? qty : BigDecimal.ZERO;
            jdbcTemplate.update("INSERT INTO goods_receipt_line (id, goods_receipt_id, line_no, purchase_order_line_id, product_id, quantity, open_putaway_qty, uom, batch_no, expiry_date, unit_cost, tax_rate, line_subtotal, line_tax, line_total, received_value, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    lineId, receiptId, lineNo, line.getPurchaseOrderLineId(), productId, qty, openPutawayQty, defaultText(line.getUom(), "EA"), line.getBatchNo(), line.getExpiryDate() == null ? null : Date.valueOf(line.getExpiryDate()), unitCost, taxRate, lineSubtotal, lineTax, lineTotal, lineTotal, nowTs(), userId);
            if (product.type().isStockControlled()) {
                applyBalance(productId, warehouseId, locationId, qty, BigDecimal.ZERO, defaultText(line.getUom(), "EA"), line.getBatchNo(), userId);
                createMovement("GOODS_RECEIPT", receiptId, receiptNo, productId, warehouseId, null, locationId, qty, defaultText(line.getUom(), "EA"), line.getBatchNo(), userId, "Goods receipt " + receiptNo);
            } else {
                audit("PRODUCT", productId, "ASSET_RECEIPT", null, qty.toPlainString(), userId, "Register received asset through Asset Management");
            }
            if (hasText(line.getPurchaseOrderLineId())) {
                jdbcTemplate.update("UPDATE purchase_order_line SET received_qty = received_qty + ?, open_qty = GREATEST(open_qty - ?, 0), status = CASE WHEN GREATEST(open_qty - ?, 0) = 0 THEN 'RECEIVED' ELSE 'PARTIAL' END WHERE id = ?",
                        qty, qty, qty, line.getPurchaseOrderLineId());
            }
            lineNo += 10;
        }
        if (hasText(request.getPurchaseOrderId())) refreshPurchaseOrderReceiptStatus(request.getPurchaseOrderId(), userId);
        audit("GOODS_RECEIPT", receiptId, "CREATE", null, null, userId, receiptNo);
        return getGoodsReceipt(receiptId);
    }

    public List<Map<String, Object>> getGoodsReceipts(String status) {
        if (hasText(status)) return jdbcTemplate.queryForList("SELECT gr.*, po.purchase_order_no, p.number AS supplier_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS supplier_name, COALESCE(l.line_count,0) AS line_count FROM goods_receipt gr LEFT JOIN purchase_order po ON po.id = gr.purchase_order_id LEFT JOIN partner p ON p.id = gr.supplier_partner_id LEFT JOIN (SELECT goods_receipt_id, COUNT(*) AS line_count FROM goods_receipt_line GROUP BY goods_receipt_id) l ON l.goods_receipt_id = gr.id WHERE gr.status = ? ORDER BY gr.created_at DESC", status.trim().toUpperCase());
        return jdbcTemplate.queryForList("SELECT gr.*, po.purchase_order_no, p.number AS supplier_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS supplier_name, COALESCE(l.line_count,0) AS line_count FROM goods_receipt gr LEFT JOIN purchase_order po ON po.id = gr.purchase_order_id LEFT JOIN partner p ON p.id = gr.supplier_partner_id LEFT JOIN (SELECT goods_receipt_id, COUNT(*) AS line_count FROM goods_receipt_line GROUP BY goods_receipt_id) l ON l.goods_receipt_id = gr.id ORDER BY gr.created_at DESC LIMIT 300");
    }

    public Map<String, Object> getGoodsReceipt(String id) {
        Map<String, Object> receipt = jdbcTemplate.queryForMap("SELECT gr.*, po.purchase_order_no, p.number AS supplier_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS supplier_name FROM goods_receipt gr LEFT JOIN purchase_order po ON po.id = gr.purchase_order_id LEFT JOIN partner p ON p.id = gr.supplier_partner_id WHERE gr.id = ?", id);
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
        requireActiveLocation(warehouseId, fromLocationId);
        LocationProfile destination = requireActiveLocation(warehouseId, toLocationId);
        if (!destination.allowPutaway()) {
            throw new IllegalArgumentException("Storage location " + destination.code() + " does not allow putaway");
        }
        LocalDate movementDate = request.getMovementDate() == null ? LocalDate.now() : request.getMovementDate();
        jdbcTemplate.update("INSERT INTO putaway (id, putaway_no, goods_receipt_id, warehouse_id, from_location_id, to_location_id, movement_date, status, notes, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                putawayId, putawayNo, request.getGoodsReceiptId(), warehouseId, fromLocationId, toLocationId, Date.valueOf(movementDate), "COMPLETED", request.getNotes(), nowTs(), userId, nowTs(), userId);
        int lineNo = 10;
        for (StockDtos.PutawayLineRequest line : request.getLines()) {
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            String productId = required(line.getProductId(), "productId");
            requirePutawayEligible(productId);
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

    // ---------------------------------------------------------------------
    // Sales Orders
    // ---------------------------------------------------------------------
    @Transactional
    public Map<String, Object> createSalesOrder(StockDtos.SalesOrderRequest request, String userId) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        if (request.getLines() == null || request.getLines().isEmpty()) throw new IllegalArgumentException("At least one order line is required");
        String salesOrderId = uuid();
        String salesOrderNo = nextNumber("SALES_ORDER", "SO");
        LocalDate orderDate = request.getOrderDate() == null ? LocalDate.now() : request.getOrderDate();
        AmountTotals totals = salesTotals(request.getLines());
        jdbcTemplate.update("INSERT INTO sales_order (id, sales_order_no, quotation_id, customer_partner_id, customer_reference, order_date, requested_delivery_date, warehouse_id, status, currency, subtotal_amount, tax_amount, total_amount, notes, created_at, created_by, updated_at, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                salesOrderId, salesOrderNo, request.getQuotationId(), request.getCustomerPartnerId(), request.getCustomerReference(), Date.valueOf(orderDate), parseDateOrNull(request.getRequestedDeliveryDate()), request.getWarehouseId(), "OPEN", defaultText(request.getCurrency(), "ZAR"), totals.subtotal, totals.tax, totals.total, request.getNotes(), nowTs(), userId, nowTs(), userId);
        int lineNo = 10;
        for (StockDtos.SalesOrderLineRequest line : request.getLines()) {
            String productId = resolveProductId(line.getProductId(), line.getProductCode());
            ProductProfile product = requireSaleable(productId);
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            BigDecimal available = product.type().isStockControlled() ? availableStock(productId, request.getWarehouseId()) : qty;
            BigDecimal allocated = available.min(qty);
            BigDecimal unitPrice = money(line.getUnitPrice());
            BigDecimal taxRate = percent(line.getTaxRate());
            BigDecimal lineSubtotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            String status = allocated.compareTo(qty) >= 0 ? "AVAILABLE" : (allocated.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "BACKORDER");
            jdbcTemplate.update("INSERT INTO sales_order_line (id, sales_order_id, line_no, product_id, product_description, quantity, allocated_qty, reserved_qty, issued_qty, uom, unit_price, tax_rate, line_subtotal, line_tax, line_total, status, notes, created_at, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    uuid(), salesOrderId, lineNo, productId, line.getDescription(), qty, allocated, BigDecimal.ZERO, BigDecimal.ZERO, defaultText(line.getUom(), "EA"), unitPrice, taxRate, lineSubtotal, lineTax, lineSubtotal.add(lineTax), status, line.getNotes(), nowTs(), userId);
            lineNo += 10;
        }
        audit("SALES_ORDER", salesOrderId, "CREATE", null, null, userId, salesOrderNo);
        return getSalesOrder(salesOrderId);
    }

    public List<Map<String, Object>> getSalesOrders(String status) {
        if (hasText(status)) return jdbcTemplate.queryForList("SELECT so.*, p.number AS customer_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS customer_name, COALESCE(l.line_count,0) AS line_count FROM sales_order so LEFT JOIN partner p ON p.id = so.customer_partner_id LEFT JOIN (SELECT sales_order_id, COUNT(*) AS line_count FROM sales_order_line GROUP BY sales_order_id) l ON l.sales_order_id = so.id WHERE so.status = ? ORDER BY so.created_at DESC", status.trim().toUpperCase());
        return jdbcTemplate.queryForList("SELECT so.*, p.number AS customer_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS customer_name, COALESCE(l.line_count,0) AS line_count FROM sales_order so LEFT JOIN partner p ON p.id = so.customer_partner_id LEFT JOIN (SELECT sales_order_id, COUNT(*) AS line_count FROM sales_order_line GROUP BY sales_order_id) l ON l.sales_order_id = so.id ORDER BY so.created_at DESC LIMIT 300");
    }

    public Map<String, Object> getSalesOrder(String id) {
        Map<String, Object> order = jdbcTemplate.queryForMap("SELECT so.*, p.number AS customer_no, TRIM(CONCAT(COALESCE(p.name2,''),' ',COALESCE(p.name3,''),' ',COALESCE(p.name1,''))) AS customer_name FROM sales_order so LEFT JOIN partner p ON p.id = so.customer_partner_id WHERE so.id = ?", id);
        order.put("lines", jdbcTemplate.queryForList("SELECT l.*, p.code AS product_code, p.description AS product_description FROM sales_order_line l LEFT JOIN product p ON p.id = l.product_id WHERE l.sales_order_id = ? ORDER BY l.line_no", id));
        return order;
    }

    @Transactional
    public Map<String, Object> reserveSalesOrder(String id, String userId) {
        ensureSalesOrderNotControlledByLayby(id);
        return reserveSalesOrderInternal(id, userId);
    }

    @Transactional
    public Map<String, Object> reserveSalesOrderForLayby(String id, String userId) {
        return reserveSalesOrderInternal(id, userId);
    }

    private Map<String, Object> reserveSalesOrderInternal(String id, String userId) {
        Map<String, Object> order = getSalesOrder(id);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) order.get("lines");
        for (Map<String, Object> line : lines) {
            BigDecimal quantity = decimal(line.get("quantity"));
            BigDecimal reserved = decimal(line.get("reserved_qty"));
            BigDecimal toReserve = quantity.subtract(reserved);
            if (toReserve.compareTo(BigDecimal.ZERO) <= 0) continue;
            String productId = text(line.get("product_id"));
            ProductProfile product = productProfile(productId);
            BigDecimal actuallyReserved = product.type().isStockControlled()
                    ? reserveStock(id, text(line.get("id")), productId, text(order.get("warehouse_id")), toReserve, userId)
                    : toReserve;
            BigDecimal newReserved = reserved.add(actuallyReserved);
            String status = newReserved.compareTo(quantity) >= 0 ? "RESERVED" : (newReserved.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "BACKORDER");
            jdbcTemplate.update("UPDATE sales_order_line SET reserved_qty = ?, allocated_qty = GREATEST(allocated_qty, ?), status = ? WHERE id = ?", newReserved, newReserved, status, line.get("id"));
        }
        refreshSalesOrderStatus(id, userId);
        audit("SALES_ORDER", id, "RESERVE", null, null, userId, null);
        return getSalesOrder(id);
    }

    @Transactional
    public Map<String, Object> releaseSalesOrderReservation(String id, String userId) {
        Map<String, Object> order = getSalesOrder(id);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) order.get("lines");
        for (Map<String, Object> line : lines) {
            BigDecimal reserved = decimal(line.get("reserved_qty"));
            if (reserved.compareTo(BigDecimal.ZERO) <= 0) continue;
            String productId = text(line.get("product_id"));
            ProductProfile product = productProfile(productId);
            if (product.type().isStockControlled()) {
                releaseStockReservation(text(line.get("id")), productId, reserved, userId);
            }
            jdbcTemplate.update("UPDATE sales_order_line SET reserved_qty = 0, allocated_qty = GREATEST(issued_qty, 0), status = CASE WHEN issued_qty >= quantity THEN 'ISSUED' ELSE 'AVAILABLE' END WHERE id = ?", line.get("id"));
        }
        jdbcTemplate.update("UPDATE sales_order SET status = CASE WHEN status='ISSUED' THEN status ELSE 'CANCELLED' END, updated_at = ?, updated_by = ? WHERE id = ?", nowTs(), userId, id);
        audit("SALES_ORDER", id, "RELEASE_RESERVATION", null, null, userId, null);
        return getSalesOrder(id);
    }

    @Transactional
    public Map<String, Object> issueSalesOrder(String id, StockDtos.SalesOrderIssueRequest request, String userId) {
        ensureSalesOrderNotControlledByLayby(id);
        return issueSalesOrderInternal(id, request, userId);
    }

    @Transactional
    public Map<String, Object> issueSalesOrderForLayby(String id, StockDtos.SalesOrderIssueRequest request, String userId) {
        return issueSalesOrderInternal(id, request, userId);
    }

    private Map<String, Object> issueSalesOrderInternal(String id, StockDtos.SalesOrderIssueRequest request, String userId) {
        Map<String, Object> order = getSalesOrder(id);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) order.get("lines");
        String warehouseId = request == null || !hasText(request.getWarehouseId()) ? text(order.get("warehouse_id")) : request.getWarehouseId();
        boolean hasStockControlledLine = lines.stream()
                .map(line -> productProfile(text(line.get("product_id"))))
                .anyMatch(product -> product.type().isStockControlled());
        if (hasStockControlledLine && !hasText(warehouseId)) {
            throw new IllegalArgumentException("warehouseId is required to issue stock-controlled products");
        }
        if (hasStockControlledLine && hasText(request == null ? null : request.getStorageLocationId())) {
            LocationProfile issueLocation = requireActiveLocation(warehouseId, request.getStorageLocationId());
            if (!issueLocation.allowPicking() || !issueLocation.availableForIssue()) {
                throw new IllegalArgumentException("Storage location " + issueLocation.code() + " does not allow stock picking or issue");
            }
        }
        String issueNo = nextNumber("STOCK_ISSUE", "ISS");
        for (Map<String, Object> line : lines) {
            BigDecimal quantity = decimal(line.get("quantity"));
            BigDecimal issued = decimal(line.get("issued_qty"));
            BigDecimal toIssue = quantity.subtract(issued);
            if (toIssue.compareTo(BigDecimal.ZERO) <= 0) continue;
            String productId = text(line.get("product_id"));
            ProductProfile product = productProfile(productId);
            if (product.type().isStockControlled()) {
                issueStock(text(line.get("id")), productId, warehouseId, request == null ? null : request.getStorageLocationId(), toIssue, userId);
                createMovement(product.type().isConsumedOnIssue() ? "CONSUMPTION_ISSUE" : "SALES_ISSUE", id, issueNo, productId, warehouseId, request == null ? null : request.getStorageLocationId(), null, toIssue, defaultText(text(line.get("uom")), "EA"), null, userId, request == null ? null : request.getNotes());
            }
            jdbcTemplate.update("UPDATE sales_order_line SET issued_qty = issued_qty + ?, reserved_qty = GREATEST(reserved_qty - ?, 0), status = CASE WHEN issued_qty + ? >= quantity THEN 'ISSUED' ELSE 'PARTIAL' END WHERE id = ?", toIssue, toIssue, toIssue, line.get("id"));
        }
        refreshSalesOrderStatus(id, userId);
        audit("SALES_ORDER", id, "ISSUE", null, issueNo, userId, request == null ? null : request.getNotes());
        return getSalesOrder(id);
    }

    @Transactional
    public Map<String, Object> updateSalesOrderStatus(String id, StockDtos.StatusUpdateRequest request, String userId) {
        ensureSalesOrderNotControlledByLayby(id);
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
        response.setOpenSalesOrders(queryInt("SELECT COUNT(*) FROM sales_order WHERE status IN ('OPEN','PARTIAL','RESERVED','BACKORDER')"));
        response.setOpenQuotations(queryInt("SELECT COUNT(*) FROM quotation WHERE status IN ('DRAFT','SENT','ACCEPTED')"));
        response.setOpenPurchaseOrders(queryInt("SELECT COUNT(*) FROM purchase_order WHERE status IN ('DRAFT','SENT','PARTIAL')"));
        response.setPendingPutaways(queryInt("SELECT COUNT(*) FROM goods_receipt_line WHERE open_putaway_qty > 0"));
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
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        resolveAuditActors(rows, "created_by");
        return rows;
    }

    private void refreshPurchaseOrderReceiptStatus(String purchaseOrderId, String userId) {
        BigDecimal open = queryBigDecimal("SELECT COALESCE(SUM(open_qty),0) FROM purchase_order_line WHERE purchase_order_id = ?", purchaseOrderId);
        BigDecimal received = queryBigDecimal("SELECT COALESCE(SUM(received_qty),0) FROM purchase_order_line WHERE purchase_order_id = ?", purchaseOrderId);
        String status = open.compareTo(BigDecimal.ZERO) <= 0 ? "RECEIVED" : (received.compareTo(BigDecimal.ZERO) > 0 ? "PARTIAL" : "SENT");
        jdbcTemplate.update("UPDATE purchase_order SET status = ?, updated_at = ?, updated_by = ? WHERE id = ?", status, nowTs(), userId, purchaseOrderId);
    }

    private void refreshSalesOrderStatus(String salesOrderId, String userId) {
        int openLines = queryInt("SELECT COUNT(*) FROM sales_order_line WHERE sales_order_id = ? AND issued_qty < quantity", salesOrderId);
        int reservedLines = queryInt("SELECT COUNT(*) FROM sales_order_line WHERE sales_order_id = ? AND reserved_qty >= quantity AND issued_qty < quantity", salesOrderId);
        int anyReserved = queryInt("SELECT COUNT(*) FROM sales_order_line WHERE sales_order_id = ? AND reserved_qty > 0 AND issued_qty < quantity", salesOrderId);
        String status = openLines == 0 ? "ISSUED" : (reservedLines == openLines ? "RESERVED" : (anyReserved > 0 ? "PARTIAL" : "OPEN"));
        jdbcTemplate.update("UPDATE sales_order SET status = ?, updated_at = ?, updated_by = ? WHERE id = ?", status, nowTs(), userId, salesOrderId);
    }

    private BigDecimal reserveStock(String salesOrderId, String salesOrderLineId, String productId, String warehouseId, BigDecimal quantity, String userId) {
        StringBuilder sql = new StringBuilder("SELECT b.id, b.available_qty FROM stock_balance b JOIN storage_location l ON l.id=b.storage_location_id AND UPPER(COALESCE(l.status,'ACTIVE'))='ACTIVE' JOIN storage_location_type t ON t.code=l.location_type AND t.active=1 AND t.allow_reservation=1 AND t.available_for_sale=1 WHERE b.product_id = ? AND b.available_qty > 0");
        List<Object> args = new ArrayList<>();
        args.add(productId);
        if (hasText(warehouseId)) { sql.append(" AND b.warehouse_id = ?"); args.add(warehouseId); }
        sql.append(" ORDER BY available_qty DESC FOR UPDATE");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        BigDecimal remaining = quantity;
        BigDecimal reserved = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal available = decimal(row.get("available_qty"));
            BigDecimal take = available.min(remaining);
            jdbcTemplate.update("UPDATE stock_balance SET reserved_qty = reserved_qty + ?, available_qty = GREATEST(available_qty - ?, 0), updated_at = ?, updated_by = ? WHERE id = ?", take, take, nowTs(), userId, row.get("id"));
            jdbcTemplate.update("""
                    INSERT INTO sales_order_stock_reservation(
                        id, sales_order_id, sales_order_line_id, stock_balance_id, reserved_qty, created_at, created_by, updated_at, updated_by
                    ) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP,?,CURRENT_TIMESTAMP,?)
                    ON DUPLICATE KEY UPDATE reserved_qty=reserved_qty+VALUES(reserved_qty), updated_at=CURRENT_TIMESTAMP, updated_by=VALUES(updated_by)
                    """, uuid(), salesOrderId, salesOrderLineId, row.get("id"), take, userId, userId);
            remaining = remaining.subtract(take);
            reserved = reserved.add(take);
        }
        return reserved;
    }

    private void releaseStockReservation(String salesOrderLineId, String productId, BigDecimal quantity, String userId) {
        List<Map<String, Object>> allocations = jdbcTemplate.queryForList("""
                SELECT r.id, r.stock_balance_id, r.reserved_qty
                  FROM sales_order_stock_reservation r
                 WHERE r.sales_order_line_id=? AND r.reserved_qty>0
                 ORDER BY r.created_at, r.id
                 FOR UPDATE
                """, salesOrderLineId);
        BigDecimal remaining = quantity;
        for (Map<String, Object> allocation : allocations) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal mapped = decimal(allocation.get("reserved_qty"));
            BigDecimal release = mapped.min(remaining);
            int updated = jdbcTemplate.update("""
                    UPDATE stock_balance
                       SET reserved_qty=GREATEST(reserved_qty-?,0),
                           available_qty=available_qty+?,
                           updated_at=?, updated_by=?
                     WHERE id=? AND product_id=?
                    """, release, release, nowTs(), userId, allocation.get("stock_balance_id"), productId);
            if (updated != 1) throw new IllegalStateException("Unable to locate reserved stock for product " + productId);
            BigDecimal mappedRemaining = mapped.subtract(release);
            if (mappedRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                jdbcTemplate.update("DELETE FROM sales_order_stock_reservation WHERE id=?", allocation.get("id"));
            } else {
                jdbcTemplate.update("UPDATE sales_order_stock_reservation SET reserved_qty=?, updated_at=?, updated_by=? WHERE id=?", mappedRemaining, nowTs(), userId, allocation.get("id"));
            }
            remaining = remaining.subtract(release);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Unable to release the layby's full reserved quantity for product " + productId);
        }
    }

    private void issueStock(String salesOrderLineId, String productId, String warehouseId, String storageLocationId, BigDecimal quantity, String userId) {
        boolean hasMappedReservations = queryInt(
                "SELECT COUNT(*) FROM sales_order_stock_reservation WHERE sales_order_line_id=? AND reserved_qty>0",
                salesOrderLineId) > 0;
        StringBuilder sql = new StringBuilder("SELECT b.id, b.on_hand_qty, b.reserved_qty, b.available_qty, COALESCE(r.reserved_qty,0) AS order_reserved_qty FROM stock_balance b JOIN storage_location l ON l.id=b.storage_location_id AND UPPER(COALESCE(l.status,'ACTIVE'))='ACTIVE' JOIN storage_location_type t ON t.code=l.location_type AND t.active=1 AND t.allow_picking=1 AND t.available_for_issue=1 LEFT JOIN sales_order_stock_reservation r ON r.stock_balance_id=b.id AND r.sales_order_line_id=? WHERE b.product_id = ? AND b.warehouse_id = ? AND b.on_hand_qty > 0");
        List<Object> args = new ArrayList<>();
        args.add(salesOrderLineId);
        args.add(productId);
        args.add(warehouseId);
        if (hasText(storageLocationId)) { sql.append(" AND b.storage_location_id = ?"); args.add(storageLocationId); }
        sql.append(" ORDER BY order_reserved_qty DESC, available_qty DESC, reserved_qty DESC");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        BigDecimal remaining = quantity;
        for (Map<String, Object> row : rows) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal onHand = decimal(row.get("on_hand_qty"));
            BigDecimal reserved = decimal(row.get("reserved_qty"));
            BigDecimal available = decimal(row.get("available_qty"));
            BigDecimal orderReserved = decimal(row.get("order_reserved_qty")).min(reserved);
            BigDecimal eligible = hasMappedReservations ? orderReserved.add(available) : onHand;
            BigDecimal take = eligible.min(onHand).min(remaining);
            if (take.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal reservedTake = hasMappedReservations ? orderReserved.min(take) : reserved.min(take);
            BigDecimal availableTake = take.subtract(reservedTake);
            jdbcTemplate.update("UPDATE stock_balance SET on_hand_qty = GREATEST(on_hand_qty - ?, 0), reserved_qty = GREATEST(reserved_qty - ?, 0), available_qty = GREATEST(available_qty - ?, 0), last_movement_at = ?, updated_at = ?, updated_by = ? WHERE id = ?",
                    take, reservedTake, availableTake, nowTs(), nowTs(), userId, row.get("id"));
            remaining = remaining.subtract(take);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) throw new IllegalArgumentException("Insufficient unreserved or layby-reserved stock to issue product " + productId);
        jdbcTemplate.update("DELETE FROM sales_order_stock_reservation WHERE sales_order_line_id=?", salesOrderLineId);
    }

    private void ensureSalesOrderNotControlledByLayby(String salesOrderId) {
        List<Map<String, Object>> laybys = jdbcTemplate.queryForList(
                "SELECT layby_no, status FROM layby_agreement WHERE sales_order_id=? LIMIT 1", salesOrderId);
        if (!laybys.isEmpty()) {
            Map<String, Object> layby = laybys.get(0);
            throw new IllegalStateException(
                    "Sales order is controlled by layby " + text(layby.get("layby_no"))
                            + " (" + text(layby.get("status")) + "). Manage it from Sales & Customers > Laybys.");
        }
    }

    private ProductProfile requireSaleable(String productId) {
        ProductProfile profile = productProfile(productId);
        if (!profile.availableForSale()) {
            throw new IllegalArgumentException("Product " + profile.code() + " is configured for internal use and cannot be added to a customer document");
        }
        return profile;
    }

    private ProductProfile requireReceivable(String productId) {
        ProductProfile profile = productProfile(productId);
        if (!profile.type().isCanBeReceived()) {
            throw new IllegalArgumentException(profile.type().getDisplayName() + " products cannot be received through Goods Receipts");
        }
        return profile;
    }

    private ProductProfile requirePutawayEligible(String productId) {
        ProductProfile profile = productProfile(productId);
        if (!profile.type().isCanBePutAway()) {
            throw new IllegalArgumentException(profile.type().getDisplayName() + " products cannot be put away into stock");
        }
        return profile;
    }

    private ProductProfile productProfile(String productId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT code, type, COALESCE(available_for_sale, 1) AS available_for_sale FROM product WHERE id = ?", productId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }
        Map<String, Object> row = rows.get(0);
        ProductTypeCode type = ProductTypeCode.requireSelectable(text(row.get("type")));
        Object saleValue = row.get("available_for_sale");
        boolean availableForSale = saleValue instanceof Boolean value
                ? value
                : saleValue instanceof Number number && number.intValue() != 0;
        return new ProductProfile(productId, text(row.get("code")), type, availableForSale);
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
        String base = " FROM stock_balance b JOIN storage_location l ON l.id=b.storage_location_id AND UPPER(COALESCE(l.status,'ACTIVE'))='ACTIVE' JOIN storage_location_type t ON t.code=l.location_type AND t.active=1 AND t.available_for_sale=1 AND t.allow_reservation=1 WHERE b.product_id = ?";
        if (hasText(warehouseId)) return queryBigDecimal("SELECT COALESCE(SUM(b.available_qty),0)" + base + " AND b.warehouse_id = ?", productId, warehouseId);
        return queryBigDecimal("SELECT COALESCE(SUM(b.available_qty),0)" + base, productId);
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

    private AmountTotals totals(List<StockDtos.CommercialLineRequest> lines) {
        AmountTotals totals = new AmountTotals();
        for (StockDtos.CommercialLineRequest line : lines) {
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            BigDecimal unit = money(line.getUnitPrice());
            BigDecimal taxRate = percent(line.getTaxRate());
            BigDecimal subtotal = unit.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            totals.subtotal = totals.subtotal.add(subtotal);
            totals.tax = totals.tax.add(tax);
            totals.total = totals.total.add(subtotal).add(tax);
        }
        return totals;
    }

    private AmountTotals salesTotals(List<StockDtos.SalesOrderLineRequest> lines) {
        AmountTotals totals = new AmountTotals();
        for (StockDtos.SalesOrderLineRequest line : lines) {
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            BigDecimal unit = money(line.getUnitPrice());
            BigDecimal taxRate = percent(line.getTaxRate());
            BigDecimal subtotal = unit.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            totals.subtotal = totals.subtotal.add(subtotal);
            totals.tax = totals.tax.add(tax);
            totals.total = totals.total.add(subtotal).add(tax);
        }
        return totals;
    }

    private AmountTotals goodsReceiptTotals(List<StockDtos.GoodsReceiptLineRequest> lines) {
        AmountTotals totals = new AmountTotals();
        for (StockDtos.GoodsReceiptLineRequest line : lines) {
            BigDecimal qty = positive(line.getQuantity(), "quantity");
            BigDecimal unit = money(line.getUnitCost());
            BigDecimal taxRate = percent(line.getTaxRate());
            BigDecimal subtotal = unit.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            totals.subtotal = totals.subtotal.add(subtotal);
            totals.tax = totals.tax.add(tax);
            totals.total = totals.total.add(subtotal).add(tax);
        }
        return totals;
    }

    private String requireLocationType(String value) {
        String code = required(value, "locationType").trim().toUpperCase(Locale.ROOT);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM storage_location_type WHERE code=? AND active=1", Integer.class, code);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Unknown or inactive storage location type: " + code);
        }
        return code;
    }

    private LocationProfile requireActiveLocation(String warehouseId, String locationId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT l.id, l.location_code, l.location_type,
                       t.allow_putaway, t.allow_picking, t.allow_reservation,
                       t.available_for_sale, t.available_for_issue,
                       t.requires_batch, t.requires_expiry_date, t.requires_serial_number,
                       t.requires_quality_release, t.restricted_access, t.temporary_location
                  FROM storage_location l
                  JOIN warehouse w ON w.id=l.warehouse_id AND UPPER(COALESCE(w.status,'ACTIVE'))='ACTIVE'
                  JOIN storage_location_type t ON t.code=l.location_type AND t.active=1
                 WHERE l.id=? AND l.warehouse_id=? AND UPPER(COALESCE(l.status,'ACTIVE'))='ACTIVE'
                """, locationId, warehouseId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Storage location is not active or does not belong to the selected warehouse");
        }
        Map<String, Object> row = rows.get(0);
        return new LocationProfile(
                text(row.get("id")), text(row.get("location_code")), text(row.get("location_type")),
                truth(row.get("allow_putaway")), truth(row.get("allow_picking")), truth(row.get("allow_reservation")),
                truth(row.get("available_for_sale")), truth(row.get("available_for_issue")),
                truth(row.get("requires_batch")), truth(row.get("requires_expiry_date")),
                truth(row.get("requires_serial_number")), truth(row.get("requires_quality_release")),
                truth(row.get("restricted_access")), truth(row.get("temporary_location")));
    }

    private boolean truth(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return value instanceof byte[] bytes && bytes.length > 0 && bytes[0] != 0;
    }

    private record LocationProfile(
            String id, String code, String type,
            boolean allowPutaway, boolean allowPicking, boolean allowReservation,
            boolean availableForSale, boolean availableForIssue,
            boolean requiresBatch, boolean requiresExpiryDate, boolean requiresSerialNumber,
            boolean requiresQualityRelease, boolean restrictedAccess, boolean temporaryLocation) {
    }

    private record ProductProfile(String id, String code, ProductTypeCode type, boolean availableForSale) {
    }

    private static class AmountTotals {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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

    private Date toSqlDate(LocalDate value) { return value == null ? null : Date.valueOf(value); }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(field + " must be greater than zero");
        return value;
    }

    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal percent(BigDecimal value) { return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal decimal(Object value) { return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()); }
    private String text(Object value) { return value == null ? null : value.toString(); }
    private String textDate(Object value) { return value == null ? null : value.toString(); }
    private String required(String value, String field) { if (!hasText(value)) throw new IllegalArgumentException(field + " is required"); return value; }
    private String defaultStatus(String status) { return hasText(status) ? status.trim().toUpperCase() : "ACTIVE"; }
    private String defaultText(String value, String defaultValue) { return hasText(value) ? value.trim() : defaultValue; }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String uuid() { return UUID.randomUUID().toString().replace("-", ""); }
    private long cents(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    private LocalDate dateValue(Object value, LocalDate fallback) {
        if (value == null) return fallback;
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof java.time.LocalDate date) return date;
        try { return LocalDate.parse(value.toString()); } catch (Exception ignored) { return fallback; }
    }

    private Timestamp nowTs() { return Timestamp.valueOf(LocalDateTime.now()); }
}
