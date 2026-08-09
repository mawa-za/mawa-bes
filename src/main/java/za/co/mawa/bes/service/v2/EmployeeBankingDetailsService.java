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
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.EmploymentRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.service.PartnerBankAccountService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeBankingDetailsService {
    private final EmploymentRepository employmentRepository;
    private final PartnerRepository partnerRepository;
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
            PartnerEntity employee = partnerRepository.findById(employment.getPartnerId()).orElse(null);
            String employeeName = displayName(employee);
            PartnerBankAccountGetDto existing = partnerBankAccountService.getBankAccounts(employment.getPartnerId());
            List<PartnerBankAccountDto> currentAccounts = existing == null
                    || existing.getPartnerBankAccountDtoList() == null
                    ? List.of()
                    : existing.getPartnerBankAccountDtoList().stream()
                    .filter(account -> account != null && "ACTIVE".equalsIgnoreCase(account.getStatus()))
                    .toList();

            Map<String, Object> payload = new LinkedHashMap<>();
            Map<String, Object> employeeSummary = new LinkedHashMap<>();
            employeeSummary.put("employeeNumber", employment.getEmployeeNumber());
            employeeSummary.put("employeeName", employeeName);
            employeeSummary.put("position", employment.getPosition());
            employeeSummary.put("department", employment.getDepartment());
            payload.put("employee", employeeSummary);
            payload.put("currentBankingDetails", currentAccounts);
            payload.put("proposedBankingDetails", details);
            payload.put("attachmentObjectIds", java.util.stream.Stream.of(employmentId, employment.getPartnerId())
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toList());

            ApprovalSubmitRequest request = new ApprovalSubmitRequest();
            request.setApprovalType(ApprovalType.EMPLOYEE_BANKING_DETAILS);
            request.setReferenceId(employmentId + "-BANK-" + System.currentTimeMillis());
            String employeeNumber = nonBlank(employment.getEmployeeNumber(), employee == null ? null : employee.getNo(), employmentId);
            request.setReferenceNo(employeeNumber);
            request.setTitle("Employee banking details change - " + employeeName
                    + " (" + employeeNumber + ")");
            request.setDescription("Compare the employee's current and proposed banking details before approval.");
            request.setRequesterId(requesterId);
            request.setPayloadJson(objectMapper.writeValueAsString(payload));
            return approvalService.submitForApproval(request);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to submit employee banking details: " + exception.getMessage(), exception);
        }
    }

    private String displayName(PartnerEntity partner) {
        if (partner == null) return "Employee";
        String type = partner.getType() == null ? "" : partner.getType().trim().toUpperCase();
        if ("ORGANISATION".equals(type) || "ORGANIZATION".equals(type) || "GROUP".equals(type)) {
            return nonBlank(partner.getName1(), partner.getNo(), "Employee");
        }
        String name = java.util.stream.Stream.of(partner.getName2(), partner.getName3(), partner.getName1())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
        return nonBlank(name, partner.getNo(), "Employee");
    }

    private String nonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "Employee";
    }

    private EmploymentEntity requireEmployment(String employmentId) {
        return employmentRepository.findById(employmentId)
                .orElseThrow(() -> new IllegalArgumentException("Employment record not found: " + employmentId));
    }
}
