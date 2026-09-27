package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.stock.StockDtos;
import za.co.mawa.bes.service.v2.StockOperationsService;
import za.co.mawa.bes.service.v2.InventoryAccessService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/v2")
public class StockOperationsControllerV2 {
    private final StockOperationsService stockOperationsService;
    private final InventoryAccessService inventoryAccessService;

    public StockOperationsControllerV2(StockOperationsService stockOperationsService, InventoryAccessService inventoryAccessService) {
        this.stockOperationsService = stockOperationsService;
        this.inventoryAccessService = inventoryAccessService;
    }

    @GetMapping("/stock/dashboard")
    public ResponseEntity<StockDtos.StockDashboardResponse> dashboard() {
        inventoryAccessService.requireWorkcentre("inventory-dashboard");
        List<String> scope = inventoryAccessService.scopedWarehouseIds();
        return ResponseEntity.ok(scope.isEmpty() ? stockOperationsService.dashboard() : stockOperationsService.dashboard(scope));
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<Map<String, Object>>> warehouses(@RequestParam(required = false) String status) {
        inventoryAccessService.requireAnyInventoryWorkcentre();
        return ResponseEntity.ok(filterWarehouseRows(stockOperationsService.getWarehouses(status), "id"));
    }

    @PostMapping("/warehouses")
    public ResponseEntity<Map<String, Object>> createWarehouse(@RequestBody StockDtos.WarehouseRequest request,
                                                               @RequestHeader(value = "X-User-Id", required = false) String userId) {
        inventoryAccessService.requireWorkcentre("inventory-setup");
        return ResponseEntity.ok(stockOperationsService.createWarehouse(request, inventoryAccessService.actorId()));
    }

    @GetMapping("/storage-locations")
    public ResponseEntity<List<Map<String, Object>>> storageLocations(@RequestParam(required = false) String warehouseId,
                                                                       @RequestParam(required = false) String status) {
        inventoryAccessService.requireAnyInventoryWorkcentre();
        inventoryAccessService.requireWarehouse(warehouseId);
        return ResponseEntity.ok(filterWarehouseRows(stockOperationsService.getStorageLocations(warehouseId, status), "warehouse_id"));
    }

    @PostMapping("/storage-locations")
    public ResponseEntity<Map<String, Object>> createStorageLocation(@RequestBody StockDtos.StorageLocationRequest request,
                                                                      @RequestHeader(value = "X-User-Id", required = false) String userId) {
        inventoryAccessService.requireWorkcentre("inventory-setup");
        inventoryAccessService.requireWarehouse(request.getWarehouseId());
        return ResponseEntity.ok(stockOperationsService.createStorageLocation(request, inventoryAccessService.actorId()));
    }

    @GetMapping("/stock")
    public ResponseEntity<List<Map<String, Object>>> stock(@RequestParam(required = false) String warehouseId,
                                                            @RequestParam(required = false) String storageLocationId,
                                                            @RequestParam(required = false) String productId,
                                                            @RequestParam(required = false) Boolean availableOnly) {
        inventoryAccessService.requireWorkcentre("stock-on-hand");
        inventoryAccessService.requireWarehouse(warehouseId);
        return ResponseEntity.ok(filterWarehouseRows(stockOperationsService.getStock(warehouseId, storageLocationId, productId, availableOnly), "warehouse_id"));
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<List<Map<String, Object>>> movements(@RequestParam(required = false) String productId,
                                                                @RequestParam(required = false) String warehouseId,
                                                                @RequestParam(required = false) String storageLocationId,
                                                                @RequestParam(required = false) String movementType,
                                                                @RequestParam(required = false) String fromDate,
                                                                @RequestParam(required = false) String toDate) {
        inventoryAccessService.requireWorkcentre("stock-movement");
        inventoryAccessService.requireWarehouse(warehouseId);
        return ResponseEntity.ok(filterMovementRows(stockOperationsService.getMovements(productId, warehouseId, storageLocationId, movementType, fromDate, toDate)));
    }

    @GetMapping("/products/{productId}/stock-movements")
    public ResponseEntity<List<Map<String, Object>>> productMovements(@PathVariable String productId) {
        inventoryAccessService.requireWorkcentre("stock-movement");
        return ResponseEntity.ok(filterMovementRows(stockOperationsService.getMovements(productId, null, null, null, null, null)));
    }

    @PostMapping("/quotations")
    public ResponseEntity<Map<String, Object>> createQuotation(@RequestBody StockDtos.QuotationRequest request,
                                                               @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.createQuotation(request, userId));
    }

    @GetMapping("/quotations")
    public ResponseEntity<List<Map<String, Object>>> quotations(@RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) String customerPartnerId) {
        return ResponseEntity.ok(stockOperationsService.getQuotations(status, customerPartnerId));
    }

    @GetMapping("/quotations/{id}")
    public ResponseEntity<Map<String, Object>> quotation(@PathVariable String id) {
        return ResponseEntity.ok(stockOperationsService.getQuotation(id));
    }

    @PutMapping("/quotations/{id}")
    public ResponseEntity<Map<String, Object>> updateQuotation(@PathVariable String id,
                                                                 @RequestBody StockDtos.QuotationRequest request,
                                                                 @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.updateQuotation(id, request, userId));
    }

    @PostMapping("/quotations/{id}/status")
    public ResponseEntity<Map<String, Object>> updateQuotationStatus(@PathVariable String id,
                                                                      @RequestBody StockDtos.StatusUpdateRequest request,
                                                                      @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.updateQuotationStatus(id, request, userId));
    }

    @PostMapping("/quotations/{id}/convert-to-sales-order")
    public ResponseEntity<Map<String, Object>> convertQuotationToSalesOrder(@PathVariable String id,
                                                                             @RequestBody(required = false) StockDtos.ConvertQuotationRequest request,
                                                                             @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.convertQuotationToSalesOrder(id, request, userId));
    }

    @PostMapping("/quotations/{id}/convert-to-invoice")
    public ResponseEntity<?> convertQuotationToInvoice(@PathVariable String id,
                                                         @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.convertQuotationToInvoice(id, userId));
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<Map<String, Object>> createPurchaseOrder(@RequestBody StockDtos.PurchaseOrderRequest request,
                                                                    @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.createPurchaseOrder(request, userId));
    }

    @GetMapping("/purchase-orders")
    public ResponseEntity<List<Map<String, Object>>> purchaseOrders(@RequestParam(required = false) String status,
                                                                     @RequestParam(required = false) String supplierPartnerId) {
        return ResponseEntity.ok(stockOperationsService.getPurchaseOrders(status, supplierPartnerId));
    }

    @GetMapping("/purchase-orders/{id}")
    public ResponseEntity<Map<String, Object>> purchaseOrder(@PathVariable String id) {
        return ResponseEntity.ok(stockOperationsService.getPurchaseOrder(id));
    }

    @PostMapping("/purchase-orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updatePurchaseOrderStatus(@PathVariable String id,
                                                                          @RequestBody StockDtos.StatusUpdateRequest request,
                                                                          @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.updatePurchaseOrderStatus(id, request, userId));
    }

    @PostMapping("/purchase-orders/{id}/goods-receipt")
    public ResponseEntity<Map<String, Object>> receivePurchaseOrder(@PathVariable String id,
                                                                     @RequestBody(required = false) StockDtos.GoodsReceiptRequest request,
                                                                     @RequestHeader(value = "X-User-Id", required = false) String userId) {
        inventoryAccessService.requireWorkcentre("goods-receipt");
        if (request != null) inventoryAccessService.requireWarehouse(request.getWarehouseId());
        return ResponseEntity.ok(stockOperationsService.createGoodsReceiptForPurchaseOrder(id, request, inventoryAccessService.actorId()));
    }

    @PostMapping("/goods-receipts")
    public ResponseEntity<Map<String, Object>> createGoodsReceipt(@RequestBody StockDtos.GoodsReceiptRequest request,
                                                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
        inventoryAccessService.requireWorkcentre("goods-receipt");
        inventoryAccessService.requireWarehouse(request.getWarehouseId());
        return ResponseEntity.ok(stockOperationsService.createGoodsReceipt(request, inventoryAccessService.actorId()));
    }

    @GetMapping("/goods-receipts")
    public ResponseEntity<List<Map<String, Object>>> goodsReceipts(@RequestParam(required = false) String status) {
        inventoryAccessService.requireWorkcentre("goods-receipt");
        return ResponseEntity.ok(filterWarehouseRows(stockOperationsService.getGoodsReceipts(status), "warehouse_id"));
    }

    @GetMapping("/goods-receipts/{id}")
    public ResponseEntity<Map<String, Object>> goodsReceipt(@PathVariable String id) {
        inventoryAccessService.requireWorkcentre("goods-receipt");
        Map<String,Object> row = stockOperationsService.getGoodsReceipt(id);
        inventoryAccessService.requireWarehouse(text(row.get("warehouse_id")));
        return ResponseEntity.ok(row);
    }

    @PostMapping("/putaways")
    public ResponseEntity<Map<String, Object>> createPutaway(@RequestBody StockDtos.PutawayRequest request,
                                                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        inventoryAccessService.requireWorkcentre("putaway");
        inventoryAccessService.requireWarehouse(request.getWarehouseId());
        return ResponseEntity.ok(stockOperationsService.createPutaway(request, inventoryAccessService.actorId()));
    }

    @GetMapping("/putaways")
    public ResponseEntity<List<Map<String, Object>>> putaways() {
        inventoryAccessService.requireWorkcentre("putaway");
        return ResponseEntity.ok(filterWarehouseRows(stockOperationsService.getPutaways(), "warehouse_id"));
    }

    @GetMapping("/putaways/{id}")
    public ResponseEntity<Map<String, Object>> putaway(@PathVariable String id) {
        inventoryAccessService.requireWorkcentre("putaway");
        Map<String,Object> row = stockOperationsService.getPutaway(id);
        inventoryAccessService.requireWarehouse(text(row.get("warehouse_id")));
        return ResponseEntity.ok(row);
    }

    @PostMapping("/sales-orders")
    public ResponseEntity<Map<String, Object>> createSalesOrder(@RequestBody StockDtos.SalesOrderRequest request,
                                                                 @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.createSalesOrder(request, userId));
    }

    @GetMapping("/sales-orders")
    public ResponseEntity<List<Map<String, Object>>> salesOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(stockOperationsService.getSalesOrders(status));
    }

    @GetMapping("/sales-orders/{id}")
    public ResponseEntity<Map<String, Object>> salesOrder(@PathVariable String id) {
        return ResponseEntity.ok(stockOperationsService.getSalesOrder(id));
    }

    @PostMapping("/sales-orders/{id}/reserve")
    public ResponseEntity<Map<String, Object>> reserveSalesOrder(@PathVariable String id,
                                                                  @RequestHeader(value = "X-User-Id", required = false) String userId) {
        inventoryAccessService.requireWorkcentre("stock-reservation");
        Map<String,Object> order = stockOperationsService.getSalesOrder(id);
        inventoryAccessService.requireWarehouse(text(order.get("warehouse_id")));
        return ResponseEntity.ok(stockOperationsService.reserveSalesOrder(id, inventoryAccessService.actorId()));
    }

    @PostMapping("/sales-orders/{id}/issue")
    public ResponseEntity<Map<String, Object>> issueSalesOrder(@PathVariable String id,
                                                                @RequestBody(required = false) StockDtos.SalesOrderIssueRequest request,
                                                                @RequestHeader(value = "X-User-Id", required = false) String userId) {
        inventoryAccessService.requireWorkcentre("stock-dispatch");
        Map<String,Object> order = stockOperationsService.getSalesOrder(id);
        String warehouseId = request != null && request.getWarehouseId() != null ? request.getWarehouseId() : text(order.get("warehouse_id"));
        inventoryAccessService.requireWarehouse(warehouseId);
        return ResponseEntity.ok(stockOperationsService.issueSalesOrder(id, request, inventoryAccessService.actorId()));
    }

    @PostMapping("/sales-orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateSalesOrderStatus(@PathVariable String id,
                                                                       @RequestBody StockDtos.StatusUpdateRequest request,
                                                                       @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.updateSalesOrderStatus(id, request, userId));
    }

    @GetMapping("/audit-trail")
    public ResponseEntity<List<Map<String, Object>>> audit(@RequestParam(required = false) String entityType,
                                                            @RequestParam(required = false) String entityId) {
        inventoryAccessService.requireWorkcentre("inventory-audit");
        return ResponseEntity.ok(stockOperationsService.auditTrail(entityType, entityId));
    }
    private List<Map<String,Object>> filterWarehouseRows(List<Map<String,Object>> rows, String warehouseField) {
        List<String> scope = inventoryAccessService.scopedWarehouseIds();
        if (scope.isEmpty()) return rows;
        Set<String> allowed = new LinkedHashSet<>(scope);
        return rows.stream().filter(row -> allowed.contains(text(row.get(warehouseField)))).collect(Collectors.toList());
    }

    private List<Map<String,Object>> filterMovementRows(List<Map<String,Object>> rows) {
        List<String> scope = inventoryAccessService.scopedWarehouseIds();
        if (scope.isEmpty()) return rows;
        Set<String> allowed = new LinkedHashSet<>(scope);
        return rows.stream().filter(row -> allowed.contains(text(row.get("source_warehouse_id")))
                || allowed.contains(text(row.get("destination_warehouse_id")))
                || allowed.contains(text(row.get("warehouse_id")))).collect(Collectors.toList());
    }

    private String text(Object value) { return value == null ? null : value.toString(); }

}
