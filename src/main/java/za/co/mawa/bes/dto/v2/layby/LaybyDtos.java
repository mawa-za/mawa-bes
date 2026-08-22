package za.co.mawa.bes.dto.v2.layby;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class LaybyDtos {
    private LaybyDtos() {}

    @Data
    public static class ConfigurationRequest {
        private Boolean enabled;
        private String defaultPaymentFrequency;
        private Integer defaultDurationMonths;
        private Boolean depositRequired;
        private BigDecimal minimumDepositPercent;
        private BigDecimal cancellationPenaltyPercent;
        private Integer defaultGraceBusinessDays;
        private Boolean requireCancellationApproval;
        private Boolean requireRefundApproval;
        private Boolean createRefundPaymentRequestOnCancellation;
        private Boolean automaticallyReserveStock;
        private Boolean allowStockShortLayby;
    }

    @Data
    public static class CreateLaybyRequest {
        private String quotationId;
        private String customerPartnerId;
        private String customerReference;
        private String warehouseId;
        private LocalDate requestedDeliveryDate;
        private String currency;
        private String paymentFrequency;
        private Integer installmentCount;
        private LocalDate firstInstallmentDate;
        private Long depositCents;
        private Boolean termsAccepted;
        private String termsAcceptedBy;
        private String notes;
        private List<LineRequest> lines = new ArrayList<>();
    }

    @Data
    public static class LineRequest {
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
    public static class PaymentRequest {
        private Long amountCents;
        private String paymentMethod;
        private LocalDate paymentDate;
        private String reference;
        private String location;
        private String employeeResponsible;
        private String deviceId;
        private String terminalId;
        private String notes;
        private String createdBy;
    }

    @Data
    public static class CancellationRequest {
        private String reasonCode;
        private String reason;
        private String refundMethod;
        private String requestedBy;
    }

    @Data
    public static class SignedCancellationFormRequest {
        private String file;
        private String extension;
    }

    @Data
    public static class RefundPaidRequest {
        private String paymentReference;
        private String notes;
        private String actionBy;
    }

    @Data
    public static class FulfilRequest {
        private String notes;
        private String actionBy;
    }
}
