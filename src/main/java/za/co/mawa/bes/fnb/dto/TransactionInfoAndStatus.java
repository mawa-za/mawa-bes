package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class TransactionInfoAndStatus {
    private String originalEndToEndId;
    private String transactionStatus;
    private List<StatusReasonInformation> statusReasonInformation;
}
