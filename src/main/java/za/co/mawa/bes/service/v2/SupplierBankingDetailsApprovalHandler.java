package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.PartnerBankAccountEditDto;
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
            PartnerBankAccountDto details = objectMapper.readValue(
                    approvalRequest.getPayloadJson(),
                    PartnerBankAccountDto.class
            );
            details.setStatus("ACTIVE");
            if (details.getId() == null || details.getId().isBlank()) {
                String bankAccountId = partnerBankAccountService.addBankAccount(details);
                approvalRequest.setReferenceId(bankAccountId);
            } else {
                PartnerBankAccountEditDto edit = new PartnerBankAccountEditDto();
                edit.setPartner(details.getPartner());
                edit.setAccountHolder(details.getAccountHolder());
                edit.setAccountNumber(details.getAccountNumber());
                edit.setAccountType(details.getAccountType());
                edit.setBankName(details.getBankName());
                edit.setBranchCode(details.getBranchCode());
                edit.setBranchName(details.getBranchName());
                edit.setValidFrom(details.getValidFrom());
                edit.setValidTo(details.getValidTo());
                edit.setStatus("ACTIVE");
                partnerBankAccountService.editBankAccount(edit, details.getId());
                approvalRequest.setReferenceId(details.getId());
            }
            approvalRequest.setDescription("Supplier banking details activated after final approval.");
        } catch (Exception ex) {
            throw new IllegalStateException("Approved supplier banking details could not be saved: " + ex.getMessage(), ex);
        }
    }
}
