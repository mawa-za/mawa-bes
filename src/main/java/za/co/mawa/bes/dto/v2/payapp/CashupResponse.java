package za.co.mawa.bes.dto.v2.payapp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CashupResponse {

    private String status;
    private String cashupId;
    private Long cashupNo;
    private String message;
    private String approvalRequestId;
}