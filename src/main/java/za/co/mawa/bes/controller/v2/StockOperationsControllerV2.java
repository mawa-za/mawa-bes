package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.stock.StockDtos;
import za.co.mawa.bes.service.v2.StockOperationsService;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/v2")
public class StockOperationsControllerV2 {
    private final StockOperationsService stockOperationsService;

    public StockOperationsControllerV2(StockOperationsService stockOperationsService) {
        this.stockOperationsService = stockOperationsService;
    }

    @GetMapping("/stock/dashboard")
    public ResponseEntity<StockDtos.StockDashboardResponse> dashboard() {
        return ResponseEntity.ok(stockOperationsService.dashboard());
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<Map<String, Object>>> warehouses(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(stockOperationsService.getWarehouses(status));
    }

    @PostMapping("/warehouses")
    public ResponseEntity<Map<String, Object>> createWarehouse(@RequestBody StockDtos.WarehouseRequest request,
                                                               @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.createWarehouse(request, userId));
    }

    @GetMapping("/storage-locations")
    public ResponseEntity<List<Map<String, Object>>> storageLocations(@RequestParam(required = false) String warehouseId,
                                                                       @RequestParam(required = false) String status) {
        return ResponseEntity.ok(stockOperationsService.getStorageLocations(warehouseId, status));
    }

    @PostMapping("/storage-locations")
    public ResponseEntity<Map<String, Object>> createStorageLocation(@RequestBody StockDtos.StorageLocationRequest request,
                                                                      @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.createStorageLocation(request, userId));
    }

    @GetMapping("/stock")
    public ResponseEntity<List<Map<String, Object>>> stock(@RequestParam(required = false) String warehouseId,
                                                            @RequestParam(required = false) String storageLocationId,
                                                            @RequestParam(required = false) String productId,
                                                            @RequestParam(required = false) Boolean availableOnly) {
        return ResponseEntity.ok(stockOperationsService.getStock(warehouseId, storageLocationId, productId, availableOnly));
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<List<Map<String, Object>>> movements(@RequestParam(required = false) String productId,
                                                                @RequestParam(required = false) String warehouseId,
                                                                @RequestParam(required = false) String storageLocationId,
                                                                @RequestParam(required = false) String movementType,
                                                                @RequestParam(required = false) String fromDate,
                                                                @RequestParam(required = false) String toDate) {
        return ResponseEntity.ok(stockOperationsService.getMovements(productId, warehouseId, storageLocationId, movementType, fromDate, toDate));
    }

    @GetMapping("/products/{productId}/stock-movements")
    public ResponseEntity<List<Map<String, Object>>> productMovements(@PathVariable String productId) {
        return ResponseEntity.ok(stockOperationsService.getMovements(productId, null, null, null, null, null));
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
        return ResponseEntity.ok(stockOperationsService.createGoodsReceiptForPurchaseOrder(id, request, userId));
    }

    @PostMapping("/goods-receipts")
    public ResponseEntity<Map<String, Object>> createGoodsReceipt(@RequestBody StockDtos.GoodsReceiptRequest request,
                                                                   @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.createGoodsReceipt(request, userId));
    }

    @GetMapping("/goods-receipts")
    public ResponseEntity<List<Map<String, Object>>> goodsReceipts(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(stockOperationsService.getGoodsReceipts(status));
    }

    @GetMapping("/goods-receipts/{id}")
    public ResponseEntity<Map<String, Object>> goodsReceipt(@PathVariable String id) {
        return ResponseEntity.ok(stockOperationsService.getGoodsReceipt(id));
    }

    @PostMapping("/putaways")
    public ResponseEntity<Map<String, Object>> createPutaway(@RequestBody StockDtos.PutawayRequest request,
                                                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.createPutaway(request, userId));
    }

    @GetMapping("/putaways")
    public ResponseEntity<List<Map<String, Object>>> putaways() {
        return ResponseEntity.ok(stockOperationsService.getPutaways());
    }

    @GetMapping("/putaways/{id}")
    public ResponseEntity<Map<String, Object>> putaway(@PathVariable String id) {
        return ResponseEntity.ok(stockOperationsService.getPutaway(id));
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
        return ResponseEntity.ok(stockOperationsService.reserveSalesOrder(id, userId));
    }

    @PostMapping("/sales-orders/{id}/issue")
    public ResponseEntity<Map<String, Object>> issueSalesOrder(@PathVariable String id,
                                                                @RequestBody(required = false) StockDtos.SalesOrderIssueRequest request,
                                                                @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ResponseEntity.ok(stockOperationsService.issueSalesOrder(id, request, userId));
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
        return ResponseEntity.ok(stockOperationsService.auditTrail(entityType, entityId));
    }
}
