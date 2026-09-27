package za.co.mawa.bes.dto.v2.stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryDtos {
    @Data
    public static class PostingRequest {
        private String documentId;
        private String movementType;
        private String referenceType;
        private String referenceId;
        private String referenceNo;
        private LocalDate postingDate;
        private String idempotencyKey;
        private String notes;
        private List<PostingLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class PostingLineRequest {
        private String productId;
        private String warehouseId;
        private String sourceWarehouseId;
        private String destinationWarehouseId;
        private String sourceLocationId;
        private String destinationLocationId;
        private String sourceStockStatus;
        private String destinationStockStatus;
        private BigDecimal quantity;
        private String uom;
        private String batchNo;
        private LocalDate expiryDate;
        private BigDecimal unitCost;
        private List<String> serialNumbers = new ArrayList<>();
    }

    @Data
    public static class InventoryDocumentRequest {
        private String documentType;
        private String warehouseId;
        private String destinationWarehouseId;
        private String referenceType;
        private String referenceId;
        private String referenceNo;
        private String reasonCode;
        private LocalDate requestedDate;
        private LocalDate postingDate;
        private String notes;
        private String idempotencyKey;
        private List<InventoryDocumentLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class InventoryDocumentLineRequest {
        private String productId;
        private BigDecimal quantity;
        private String uom;
        private String sourceLocationId;
        private String destinationLocationId;
        private String sourceStockStatus;
        private String destinationStockStatus;
        private String batchNo;
        private LocalDate expiryDate;
        private BigDecimal unitCost;
        private List<String> serialNumbers = new ArrayList<>();
        private String notes;
    }

    @Data
    public static class StockCountCreateRequest {
        private String warehouseId;
        private String storageLocationId;
        private String countType;
        private Boolean blindCount;
        private Boolean freezeStock;
        private String notes;
    }

    @Data
    public static class StockCountUpdateRequest {
        private List<StockCountEntryRequest> lines = new ArrayList<>();
    }

    @Data
    public static class StockCountEntryRequest {
        private String lineId;
        private BigDecimal countedQty;
        private String notes;
    }

    @Data
    public static class ReservationRequest {
        private String sourceType;
        private String sourceId;
        private String sourceLineId;
        private String productId;
        private String warehouseId;
        private String storageLocationId;
        private String batchNo;
        private String stockStatus;
        private BigDecimal quantity;
        private String uom;
        private String priority;
        private String requiredAt;
    }
    @Data
    public static class WarehouseTransferReceiptRequest {
        private Boolean finalReceipt;
        private String notes;
        private List<WarehouseTransferReceiptLineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class WarehouseTransferReceiptLineRequest {
        private String lineId;
        private BigDecimal receivedQty;
        private BigDecimal damagedQty;
        private List<String> serialNumbers = new ArrayList<>();
        private List<String> damagedSerialNumbers = new ArrayList<>();
    }

    @Data
    public static class UomConversionRequest {
        private String productId;
        private String uom;
        private BigDecimal conversionFactorToBase;
        private String description;
        private Boolean active;
    }

    @Data
    public static class ProductWarehousePolicyRequest {
        private String productId;
        private String warehouseId;
        private BigDecimal minimumQty;
        private BigDecimal maximumQty;
        private BigDecimal reorderPoint;
        private BigDecimal reorderQty;
        private BigDecimal safetyStockQty;
        private String preferredSupplierPartnerId;
        private Integer leadTimeDays;
        private String preferredPickingLocationId;
        private Integer expiryWarningDays;
        private Integer minimumShelfLifeDays;
    }

    @Data
    public static class ReasonCodeRequest {
        private String code;
        private String description;
        private String category;
        private Boolean requiresNotes;
        private Boolean requiresAttachment;
        private Boolean requiresApproval;
        private Boolean active;
    }

}
