package za.co.mawa.bes.dto.v2.stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StockDtos {
    @Data
    public static class CommercialLineRequest {
        private String id;
        private String productId;
        private String productCode;
        private String description;
        private BigDecimal quantity;
        private String uom;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private String notes;
    }

    @Data
    public static class QuotationRequest {
        private String customerPartnerId;
        private String customerReference;
        private LocalDate quotationDate;
        private LocalDate validUntil;
        private LocalDate requestedDeliveryDate;
        private String currency;
        private String status;
        private String notes;
        private List<CommercialLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class ConvertQuotationRequest {
        private String warehouseId;
        private LocalDate requestedDeliveryDate;
        private String notes;
    }

    @Data
    public static class PurchaseOrderRequest {
        private String supplierPartnerId;
        private String supplierReference;
        private LocalDate orderDate;
        private LocalDate expectedDeliveryDate;
        private String warehouseId;
        private String receivingLocationId;
        private String currency;
        private String status;
        private String notes;
        private List<CommercialLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class GoodsReceiptRequest {
        private String purchaseOrderId;
        private String purchaseOrderNo;
        private String warehouseId;
        private String storageLocationId;
        private String supplierPartnerId;
        private String supplierReference;
        private LocalDate receiptDate;
        private String notes;
        private List<GoodsReceiptLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class GoodsReceiptLineRequest {
        private String purchaseOrderLineId;
        private String productId;
        private String productCode;
        private String description;
        private BigDecimal quantity;
        private String uom;
        private String batchNo;
        private LocalDate expiryDate;
        private BigDecimal unitCost;
    }

    @Data
    public static class PutawayRequest {
        private String goodsReceiptId;
        private String warehouseId;
        private String fromLocationId;
        private String toLocationId;
        private LocalDate movementDate;
        private String notes;
        private List<PutawayLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class PutawayLineRequest {
        private String goodsReceiptLineId;
        private String productId;
        private BigDecimal quantity;
        private String uom;
        private String batchNo;
    }

    @Data
    public static class SalesOrderRequest {
        private String quotationId;
        private String customerPartnerId;
        private String customerReference;
        private LocalDate orderDate;
        private String requestedDeliveryDate;
        private String warehouseId;
        private String currency;
        private String notes;
        private List<SalesOrderLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class SalesOrderLineRequest {
        private String productId;
        private String productCode;
        private String description;
        private BigDecimal quantity;
        private String uom;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private String notes;
    }

    @Data
    public static class SalesOrderIssueRequest {
        private String warehouseId;
        private String storageLocationId;
        private String notes;
    }

    @Data
    public static class StatusUpdateRequest {
        private String status;
        private String notes;
    }

    @Data
    public static class WarehouseRequest {
        private String warehouseCode;
        private String name;
        private String description;
        private String status;
    }

    @Data
    public static class StorageLocationRequest {
        private String warehouseId;
        private String locationCode;
        private String name;
        private String locationType;
        private String status;
    }

    @Data
    public static class StockDashboardResponse {
        private BigDecimal totalStockQuantity = BigDecimal.ZERO;
        private Integer productCount = 0;
        private Integer lowStockCount = 0;
        private Integer goodsReceiptsToday = 0;
        private Integer stockMovementsToday = 0;
        private Integer openSalesOrders = 0;
        private Integer openQuotations = 0;
        private Integer openPurchaseOrders = 0;
        private Integer pendingPutaways = 0;
        private Integer activeWarehouses = 0;
        private List<Map<String, Object>> stockByWarehouse = new ArrayList<>();
        private List<Map<String, Object>> lowStock = new ArrayList<>();
        private List<Map<String, Object>> recentMovements = new ArrayList<>();
        private List<Map<String, Object>> userActivity = new ArrayList<>();
    }
}
