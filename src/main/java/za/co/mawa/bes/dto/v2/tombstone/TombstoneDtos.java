package za.co.mawa.bes.dto.v2.tombstone;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TombstoneDtos {
    private TombstoneDtos() {}

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemRequest {
        private String productId;
        private String itemType;
        private String description;
        private String material;
        private String colour;
        private String dimensions;
        private String inscriptionText;
        private BigDecimal quantity;
        private Long unitPriceCents;
        private Long discountCents;
        private Long taxCents;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateOrderRequest {
        private String customerPartnerId;
        private String membershipId;
        private String deceasedPartnerId;
        private String deceasedName;
        private String funeralServiceId;
        private String cemeteryName;
        private String cemeteryArea;
        private String graveNumber;
        private BigDecimal graveLatitude;
        private BigDecimal graveLongitude;
        private String salesArea;
        private String workcenterId;
        private LocalDate expectedInstallationDate;
        private String fundingMethod;
        private Long discountCents;
        private String notes;
        @Builder.Default private List<ItemRequest> items = new ArrayList<>();
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateOrderRequest {
        private String customerPartnerId;
        private String membershipId;
        private String deceasedPartnerId;
        private String deceasedName;
        private String funeralServiceId;
        private String cemeteryName;
        private String cemeteryArea;
        private String graveNumber;
        private BigDecimal graveLatitude;
        private BigDecimal graveLongitude;
        private String salesArea;
        private String workcenterId;
        private LocalDate expectedInstallationDate;
        private String fundingMethod;
        private Long discountCents;
        private String notes;
        private List<ItemRequest> items;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FundingAllocationRequest {
        private String fundingType;
        private String sourceType;
        private String sourceId;
        private String sourceNo;
        private Long allocatedAmountCents;
        private Long confirmedAmountCents;
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LaybyAgreementRequest {
        private Long depositRequiredCents;
        private Long installmentAmountCents;
        private String paymentFrequency;
        private LocalDate startDate;
        private LocalDate expectedSettlementDate;
        private Integer gracePeriodDays;
        private Long administrationFeeCents;
        private String termsAcceptedBy;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LaybyPaymentRequest {
        private String receiptId;
        private Long amountCents;
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SiteAssessmentRequest {
        private String status;
        private LocalDateTime scheduledAt;
        private LocalDateTime assessedAt;
        private String assessorPartnerId;
        private String cemeteryName;
        private String graveNumber;
        private BigDecimal graveLatitude;
        private BigDecimal graveLongitude;
        private Integer graveLengthMm;
        private Integer graveWidthMm;
        private String foundationCondition;
        private String accessRestrictions;
        private String cemeteryRules;
        private Boolean permitRequired;
        private String permitReference;
        private Boolean permitApproved;
        private BigDecimal travelDistanceKm;
        private String additionalWorkRequired;
        private Long additionalCostCents;
        private List<String> photoAttachmentIds;
        private String failureReason;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AmendmentRequest {
        private String reason;
        private Long amountDeltaCents;
        private String supportingAttachmentId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AmendmentDecisionRequest {
        private String decision;
        private String responseNotes;
        private String actionBy;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DesignRequest {
        private String status;
        private String inscriptionText;
        private String fontName;
        private String layoutNotes;
        private List<String> symbols;
        private String material;
        private String colour;
        private String dimensions;
        private String designAttachmentId;
        private String changeRequest;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DesignApprovalRequest {
        private String approvalMethod;
        private String approvalReference;
        private String approvedBy;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProductionJobRequest {
        private String designId;
        private Boolean internalProduction;
        private String supplierPartnerId;
        private String purchaseOrderId;
        private LocalDate plannedStartDate;
        private LocalDate plannedCompletionDate;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SupplierPaymentRequest {
        private Long amountCents;
        private String payeeName;
        private String paymentMethod;
        private String bankName;
        private String accountHolder;
        private String accountNumber;
        private String branchCode;
        private String accountType;
        private String milestone;
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatusUpdateRequest {
        private String status;
        private String reason;
        private String qualityCheckedBy;
        private String qualityNotes;
        private LocalDateTime scheduledStartAt;
        private LocalDateTime scheduledEndAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TeamMemberRequest {
        private String employeePartnerId;
        private String teamRole;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InstallationMaterialRequest {
        private String productId;
        private String description;
        private BigDecimal quantity;
        private String uom;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InstallationRequest {
        private String productionJobId;
        private LocalDateTime scheduledStartAt;
        private LocalDateTime scheduledEndAt;
        private String cemeteryName;
        private String graveNumber;
        private String assignedVehicleId;
        private String contactPerson;
        private String contactNumber;
        private String permitReference;
        private String instructions;
        @Builder.Default private List<TeamMemberRequest> team = new ArrayList<>();
        @Builder.Default private List<InstallationMaterialRequest> materials = new ArrayList<>();
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChecklistUpdateRequest {
        private Boolean completed;
        private String notes;
        private String evidenceAttachmentId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InstallationCompletionRequest {
        private List<String> beforePhotoAttachmentIds;
        private List<String> afterPhotoAttachmentIds;
        private String customerRepresentativeName;
        private String customerSignatureAttachmentId;
        private String installerSignatureAttachmentId;
        private String completionNotes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AcceptanceRequest {
        private String acceptedBy;
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReworkRequest {
        private String reason;
        private LocalDateTime scheduledStartAt;
        private LocalDateTime scheduledEndAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderResponse {
        private String id;
        private String orderNo;
        private String customerPartnerId;
        private String membershipId;
        private String deceasedPartnerId;
        private String deceasedName;
        private String funeralServiceId;
        private String cemeteryName;
        private String cemeteryArea;
        private String graveNumber;
        private BigDecimal graveLatitude;
        private BigDecimal graveLongitude;
        private String salesArea;
        private String workcenterId;
        private LocalDate expectedInstallationDate;
        private String fundingMethod;
        private String status;
        private String fundingStatus;
        private String productionStatus;
        private String installationStatus;
        private Long subtotalCents;
        private Long taxCents;
        private Long discountCents;
        private Long totalCents;
        private Long confirmedFundingCents;
        private Long balanceCents;
        private String invoiceId;
        private String notes;
        private String cancellationReason;
        private LocalDateTime cancelledAt;
        private LocalDateTime createdAt;
        private String createdBy;
        private LocalDateTime updatedAt;
        private String updatedBy;
        @Builder.Default private List<Map<String, Object>> items = new ArrayList<>();
        @Builder.Default private List<Map<String, Object>> fundingAllocations = new ArrayList<>();
        private Map<String, Object> laybyAgreement;
        @Builder.Default private List<Map<String, Object>> laybyInstallments = new ArrayList<>();
        @Builder.Default private List<Map<String, Object>> assessments = new ArrayList<>();
        @Builder.Default private List<Map<String, Object>> amendments = new ArrayList<>();
        @Builder.Default private List<Map<String, Object>> designs = new ArrayList<>();
        @Builder.Default private List<Map<String, Object>> productionJobs = new ArrayList<>();
        @Builder.Default private List<Map<String, Object>> installations = new ArrayList<>();
        @Builder.Default private List<Map<String, Object>> statusHistory = new ArrayList<>();
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardResponse {
        private long totalOrders;
        private long awaitingFunding;
        private long partiallyFunded;
        private long fullyFunded;
        private long assessmentPending;
        private long designPending;
        private long inProduction;
        private long readyForInstallation;
        private long scheduledInstallations;
        private long reworkRequired;
        private long completed;
        private Long outstandingLaybyCents;
    }
}
