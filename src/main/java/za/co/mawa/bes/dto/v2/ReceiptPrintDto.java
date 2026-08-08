package za.co.mawa.bes.dto.v2;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ReceiptPrintDto {

    private String receiptNo;

    private String traceId;

    private String paymentBatchNo;

    private String sourceType;

    private String membershipId;
    private String memberName;
    private String membershipNo;
    private String identityNumber;
    private String planName;

    private String premiumPeriodYYYYMM;

    private String periodDescription;

    private String invoiceId;
    private String invoiceNo;
    private String invoiceReference;
    private String customerName;
    private String customerNumber;

    private String groupSocietyId;
    private String groupSocietyNo;
    private String groupSocietyName;
    private String groupSocietyType;

    private String referenceType;
    private String referenceNo;

    private Long amountCents;

    private String paymentMethod;

    private LocalDateTime receiptDate;

    private String location;

    private String employeeResponsible;

    private String deviceId;

    private String terminalId;

    private String syncStatus;

    private String status;

    private Integer printCount;
}
