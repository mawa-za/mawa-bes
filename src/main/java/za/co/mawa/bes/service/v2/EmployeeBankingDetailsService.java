package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.PartnerBankAccountGetDto;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.entity.EmploymentEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.service.PartnerBankAccountService;

@Service
@RequiredArgsConstructor
public class EmployeeBankingDetailsService {
    private final EmploymentRepository employmentRepository;
    private final PartnerBankAccountService partnerBankAccountService;
    private final ApprovalService approvalService;
    private final UniversalBranchCodeService universalBranchCodeService;
    private final ReferenceDataValidationService referenceDataValidationService;
    private final ObjectMapper objectMapper;

    public PartnerBankAccountGetDto get(String employmentId) {
        EmploymentEntity employment = requireEmployment(employmentId);
        return partnerBankAccountService.getBankAccounts(employment.getPartnerId());
    }

    @Transactional
    public ApprovalRequestResponse submit(
            String employmentId,
            PartnerBankAccountDto details,
            String requesterId
    ) {
        EmploymentEntity employment = requireEmployment(employmentId);
        if (details == null) throw new IllegalArgumentException("Banking details are required");
        if (requesterId == null || requesterId.isBlank()) requesterId = "SYSTEM";

        details.setPartner(employment.getPartnerId());
        details.setBankName(referenceDataValidationService.requireOption(
                "BANK-NAME", details.getBankName(), "Bank name"));
        details.setAccountType(referenceDataValidationService.requireOption(
                "BANK-ACCOUNT-TYPE", details.getAccountType(), "Bank account type"));
        if (details.getAccountHolder() == null || details.getAccountHolder().isBlank()) {
            throw new IllegalArgumentException("Account holder is required");
        }
        if (details.getAccountNumber() == null || !details.getAccountNumber().matches("\\d{5,20}")) {
            throw new IllegalArgumentException("Account number must contain 5 to 20 numeric digits");
        }
        details.setBranchCode(universalBranchCodeService.resolve(details.getBankName()));
        details.setBranchName("Universal Branch");
        details.setStatus("PENDING_APPROVAL");

        try {
            ApprovalSubmitRequest request = new ApprovalSubmitRequest();
            request.setApprovalType(ApprovalType.EMPLOYEE_BANKING_DETAILS);
            request.setReferenceId(employmentId + "-BANK-" + System.currentTimeMillis());
            request.setReferenceNo(employment.getEmployeeNumber());
            request.setTitle("Employee banking-details approval");
            request.setDescription("Approve banking details for employee " + employment.getEmployeeNumber());
            request.setRequesterId(requesterId);
            request.setPayloadJson(objectMapper.writeValueAsString(details));
            return approvalService.submitForApproval(request);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to submit employee banking details: " + exception.getMessage(), exception);
        }
    }

    private EmploymentEntity requireEmployment(String employmentId) {
        return employmentRepository.findById(employmentId)
                .orElseThrow(() -> new IllegalArgumentException("Employment record not found: " + employmentId));
    }
}
