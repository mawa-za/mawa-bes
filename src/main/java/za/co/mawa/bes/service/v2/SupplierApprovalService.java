package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.dto.partner.PartnerInboundDto;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.service.PartnerServiceV2;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierApprovalService {

    private final ApprovalService approvalService;
    private final PartnerServiceV2 partnerServiceV2;
    private final ObjectMapper objectMapper;

    public ApprovalRequestResponse submitSupplierOnboarding(
            PartnerInboundDto supplier,
            String requesterId
    ) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier details are required");
        }
        supplier.setPartnerRole("SUPPLIER");
        String referenceId = UUID.randomUUID().toString();
        String supplierName = firstNonBlank(supplier.getName1(), supplier.getName2(), "New supplier");

        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(ApprovalType.SUPPLIER_ONBOARDING);
        request.setReferenceId(referenceId);
        request.setReferenceNo("SUP-PENDING-" + referenceId.substring(0, 8).toUpperCase(Locale.ROOT));
        request.setTitle("New supplier: " + supplierName);
        request.setDescription("Approve supplier onboarding before the supplier can be selected in transactions.");
        request.setRequesterId(requireRequester(requesterId));
        request.setPayloadJson(toJson(supplier));
        return approvalService.submitForApproval(request);
    }

    public ApprovalRequestResponse submitBankingDetails(
            String partnerId,
            PartnerBankAccountDto bankingDetails,
            String requesterId
    ) {
        if (partnerId == null || partnerId.isBlank()) {
            throw new IllegalArgumentException("Supplier is required");
        }
        if (bankingDetails == null) {
            throw new IllegalArgumentException("Banking details are required");
        }
        boolean supplier = partnerServiceV2.getRoles(partnerId).stream()
                .anyMatch(role -> "SUPPLIER".equalsIgnoreCase(role));
        if (!supplier) {
            throw new IllegalArgumentException("Banking approval is only available for suppliers");
        }

        bankingDetails.setPartner(partnerId);
        String referenceId = UUID.randomUUID().toString();
        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(ApprovalType.SUPPLIER_BANKING_DETAILS);
        request.setReferenceId(referenceId);
        request.setReferenceNo("SUP-BANK-" + referenceId.substring(0, 8).toUpperCase(Locale.ROOT));
        request.setTitle("Supplier banking details approval");
        request.setDescription("Approve supplier banking details before they can be used for payment.");
        request.setRequesterId(requireRequester(requesterId));
        request.setPayloadJson(toJson(bankingDetails));
        return approvalService.submitForApproval(request);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to capture approval payload", ex);
        }
    }

    private String requireRequester(String requesterId) {
        if (requesterId == null || requesterId.isBlank()) {
            throw new IllegalArgumentException("Requester user is required");
        }
        return requesterId;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "Supplier";
    }
}
