package za.co.mawa.bes.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PremiumInboundDto implements Serializable {
    private String id;
    private String deviceReceiptId;
    private String receiptNo;
    private String memberId;
    private String paymentPeriod;
    private int amountCents;
    private String paymentMethod;
    private String createdAt;
    private String userId;
    private String deviceId;
}
