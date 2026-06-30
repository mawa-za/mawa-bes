package za.co.mawa.bes.dto.v2.payapp;

import lombok.Data;

@Data
public class CashupSubmitForApprovalRequest {
    private String requesterId;
    private String comments;
}
