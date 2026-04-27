package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class BankPaymentResponse implements Serializable {
    private String instructionId;
    private GroupHeader groupHeader;
    private OriginalGroupHeader originalGroupHeader;
    private String groupStatus;
    private List<StatusReasonInformation> statusReasonInformation;
    private List<OriginalPaymentInformation> originalPaymentInformation;
}
