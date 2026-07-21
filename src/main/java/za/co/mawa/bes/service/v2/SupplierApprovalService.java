package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.dto.partner.PartnerInboundDto;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.supplier.SupplierOnboardingRequest;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.service.PartnerServiceV2;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierApprovalService {

    private final ApprovalService approvalService;
    private final PartnerServiceV2 partnerServiceV2;
    private final AttachmentRepository attachmentRepository;
    private final ObjectMapper objectMapper;

    public ApprovalRequestResponse submitSupplierOnboarding(
            SupplierOnboardingRequest onboarding,
            String requesterId
    ) {
        if (onboarding == null) {
            throw new IllegalArgumentException("Supplier onboarding details are required");
        }
        String onboardingRequestId = requireText(
                onboarding.getOnboardingRequestId(), "Supplier onboarding request ID is required");
        if (!onboardingRequestId.startsWith("supplier-onboarding-")) {
            throw new IllegalArgumentException("Invalid supplier onboarding request ID");
        }
        if (!Boolean.TRUE.equals(onboarding.getSupportingDocumentsComplete())) {
            throw new IllegalArgumentException(
                    "Confirm that all required supporting documents have been attached before submission");
        }
        PartnerInboundDto supplier = onboarding.getSupplier();
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier details are required");
        }
        PartnerBankAccountDto banking = onboarding.getBankingDetails();
        validateBankingDetails(banking);
        validateSupportingDocuments(onboardingRequestId);
        normaliseSupplier(supplier);

        banking.setPartner(null);
        banking.setStatus("PENDING_APPROVAL");
        onboarding.setOnboardingRequestId(onboardingRequestId);
        onboarding.setSupplier(supplier);
        onboarding.setBankingDetails(banking);

        String supplierName = firstNonBlank(supplier.getName1(), supplier.getName2(), "New supplier");
        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(ApprovalType.SUPPLIER_ONBOARDING);
        request.setReferenceId(onboardingRequestId);
        request.setReferenceNo("SUP-PENDING-" + shortReference(onboardingRequestId));
        request.setTitle("New supplier: " + supplierName);
        request.setDescription(
                "Approve supplier onboarding. Supporting documents and banking details were captured; " +
                "banking approval will only be created after the supplier is approved.");
        request.setRequesterId(requireRequester(requesterId));
        request.setPayloadJson(toJson(onboarding));
        return approvalService.submitForApproval(request);
    }

    public ApprovalRequestResponse submitBankingDetails(
            String partnerId,
            PartnerBankAccountDto bankingDetails,
            String requesterId
    ) {
        if (!StringUtils.hasText(partnerId)) {
            throw new IllegalArgumentException("Supplier is required");
        }
        validateBankingDetails(bankingDetails);
        boolean supplier = partnerServiceV2.getRoles(partnerId).stream()
                .anyMatch(role -> "SUPPLIER".equalsIgnoreCase(role));
        if (!supplier) {
            throw new IllegalArgumentException("Banking approval is only available for approved suppliers");
        }

        bankingDetails.setPartner(partnerId);
        bankingDetails.setStatus("PENDING_APPROVAL");
        String referenceId = UUID.randomUUID().toString();
        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(ApprovalType.SUPPLIER_BANKING_DETAILS);
        request.setReferenceId(referenceId);
        request.setReferenceNo("SUP-BANK-" + shortReference(referenceId));
        request.setTitle("Supplier banking details approval");
        request.setDescription("Approve supplier banking details before they can be used for payment.");
        request.setRequesterId(requireRequester(requesterId));
        request.setPayloadJson(toJson(bankingDetails));
        return approvalService.submitForApproval(request);
    }

    private void normaliseSupplier(PartnerInboundDto supplier) {
        supplier.setPartnerRole("SUPPLIER");
        String partnerType = requireText(supplier.getPartnerType(), "Supplier type is required")
                .trim().toUpperCase(Locale.ROOT);
        if (!partnerType.equals("INDIVIDUAL")
                && !partnerType.equals("ORGANISATION")
                && !partnerType.equals("GROUP")) {
            throw new IllegalArgumentException("Supplier type must be INDIVIDUAL, ORGANISATION, or GROUP");
        }
        supplier.setPartnerType(partnerType);
        supplier.setName1(requireText(supplier.getName1(),
                partnerType.equals("INDIVIDUAL") ? "Supplier last name is required" : "Supplier name is required"));
        supplier.setIdentityNumber(requireText(supplier.getIdentityNumber(),
                partnerType.equals("INDIVIDUAL") ? "Identity number is required" : "Registration number is required"));
        supplier.setIdentityType(partnerType.equals("INDIVIDUAL") ? "ID" : "REGISTRATION");

        if (partnerType.equals("INDIVIDUAL")) {
            supplier.setName2(requireText(supplier.getName2(), "Supplier first name is required"));
        } else {
            // Prevent hidden individual fields from being persisted for organisations/groups.
            supplier.setName2(null);
            supplier.setName3(null);
            supplier.setTitle(null);
            supplier.setBirthDate(null);
            supplier.setMaritalStatus(null);
            supplier.setGender(null);
            supplier.setLanguage(null);
        }
    }

    private void validateSupportingDocuments(String onboardingRequestId) {
        if (attachmentRepository.findByObjectId(onboardingRequestId).isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one supporting document must be attached before supplier onboarding is submitted");
        }
    }

    private void validateBankingDetails(PartnerBankAccountDto banking) {
        if (banking == null) {
            throw new IllegalArgumentException("Supplier banking details are required");
        }
        banking.setAccountHolder(requireText(banking.getAccountHolder(), "Account holder is required"));
        banking.setBankName(requireText(banking.getBankName(), "Bank name is required"));
        banking.setAccountNumber(requireText(banking.getAccountNumber(), "Account number is required"));
        banking.setBranchCode(requireText(banking.getBranchCode(), "Branch code is required"));
        banking.setAccountType(requireText(banking.getAccountType(), "Account type is required")
                .toUpperCase(Locale.ROOT));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to capture approval payload", ex);
        }
    }

    private String requireRequester(String requesterId) {
        return requireText(requesterId, "Requester user is required");
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String shortReference(String value) {
        String compact = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return compact.substring(0, Math.min(8, compact.length()));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return "Supplier";
    }
}
