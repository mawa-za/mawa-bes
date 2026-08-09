package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.service.PartnerBankAccountService;

@Component
@RequiredArgsConstructor
public class SupplierBankingDetailsApprovalHandler implements ApprovalCompletionHandler {

    private final ObjectMapper objectMapper;
    private final PartnerBankAccountService partnerBankAccountService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.SUPPLIER_BANKING_DETAILS;
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        try {
            JsonNode payload = objectMapper.readTree(approvalRequest.getPayloadJson());
            JsonNode proposed = payload != null && payload.has("proposedBankingDetails")
                    ? payload.get("proposedBankingDetails") : payload;
            PartnerBankAccountDto details = objectMapper.treeToValue(proposed, PartnerBankAccountDto.class);
            details.setStatus("ACTIVE");
            String bankAccountId = partnerBankAccountService.activateApprovedBankAccount(details);
            approvalRequest.setReferenceId(bankAccountId);
            approvalRequest.setDescription(
                    "Supplier banking details activated after final approval; previous active banking details were closed.");
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Approved supplier banking details could not be saved: " + ex.getMessage(), ex);
        }
    }

}
