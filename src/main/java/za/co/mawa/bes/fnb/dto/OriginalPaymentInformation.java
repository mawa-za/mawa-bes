package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class OriginalPaymentInformation {
    private String originalPaymentInformationId;
    private String paymentInformationStatus;
    private List<StatusReasonInformation> statusReasonInformation;
    private List<TransactionInfoAndStatus> transactionInfoAndStatus;
}
