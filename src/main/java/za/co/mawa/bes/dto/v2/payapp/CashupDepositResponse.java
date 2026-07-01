package za.co.mawa.bes.dto.v2.payapp;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CashupDepositResponse {
    private String id;
    private String cashupId;
    private LocalDate depositDate;
    private Long amountCents;
    private String paymentMethod;
    private String bankName;
    private String referenceNo;
    private String notes;
    private String createdBy;
}
