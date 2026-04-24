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
public class PremiumOutboundDto implements Serializable {
    private String deviceReceiptId;
    private String receiptNumber;
    private String memberId;
    private String paymentPeriod;
    private int amountCents;
    private String paymentMethod;
    private String paidAt;
    private String capturedByUserId;
    private String deviceId;
}
