package za.co.mawa.bes.dto.v2.stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StockDtos {
    @Data
    public static class GoodsReceiptRequest {
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
        private String productId;
        private String productCode;
        private String description;
        private BigDecimal quantity;
        private String uom;
        private String batchNo;
        private LocalDate expiryDate;
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
        private String customerPartnerId;
        private LocalDate orderDate;
        private String requestedDeliveryDate;
        private String warehouseId;
        private String notes;
        private List<SalesOrderLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class SalesOrderLineRequest {
        private String productId;
        private String productCode;
        private BigDecimal quantity;
        private String uom;
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
        private Integer activeWarehouses = 0;
        private List<Map<String, Object>> stockByWarehouse = new ArrayList<>();
        private List<Map<String, Object>> lowStock = new ArrayList<>();
        private List<Map<String, Object>> recentMovements = new ArrayList<>();
        private List<Map<String, Object>> userActivity = new ArrayList<>();
    }
}
