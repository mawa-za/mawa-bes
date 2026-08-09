package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.service.PartnerBankAccountService;

@Component
@RequiredArgsConstructor
public class EmployeeBankingDetailsApprovalHandler implements ApprovalCompletionHandler {
    private final ObjectMapper objectMapper;
    private final PartnerBankAccountService partnerBankAccountService;

    @Override
    public ApprovalType supports() {
        return ApprovalType.EMPLOYEE_BANKING_DETAILS;
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        try {
            PartnerBankAccountDto details = objectMapper.readValue(
                    approvalRequest.getPayloadJson(), PartnerBankAccountDto.class);
            details.setStatus("ACTIVE");
            partnerBankAccountService.activateApprovedBankAccount(details);
            approvalRequest.setDescription("Employee banking details activated after final approval.");
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Approved employee banking details could not be activated: " + exception.getMessage(), exception);
        }
    }
}
