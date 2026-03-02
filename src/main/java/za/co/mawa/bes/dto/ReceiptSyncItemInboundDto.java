package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReceiptSyncItemInboundDto implements Serializable {
    private String clientReceiptId;
    private int receiptNo;
    private String userId;
    private String memberId;
    private String planId;
    private String planName;
    private int amountCents;
    private String periodYYYYMM;
    private String paymentMethod;
    private String createdAt;
}
