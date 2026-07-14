package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.partner.PartnerInboundDto;
import za.co.mawa.bes.entity.PartnerViewEntity;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.service.PartnerServiceV2;

@Component
@RequiredArgsConstructor
public class SupplierOnboardingApprovalHandler implements ApprovalCompletionHandler {

    private final ObjectMapper objectMapper;
    private final PartnerServiceV2 partnerServiceV2;

    @Override
    public ApprovalType supports() {
        return ApprovalType.SUPPLIER_ONBOARDING;
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        try {
            PartnerInboundDto supplier = objectMapper.readValue(
                    approvalRequest.getPayloadJson(),
                    PartnerInboundDto.class
            );
            supplier.setPartnerRole("SUPPLIER");
            PartnerViewEntity created = partnerServiceV2.create(supplier);
            approvalRequest.setReferenceId(created.getPartnerId());
            approvalRequest.setReferenceNo(created.getPartnerNo());
            approvalRequest.setDescription("Supplier created after final approval.");
        } catch (Exception ex) {
            throw new IllegalStateException("Approved supplier could not be created: " + ex.getMessage(), ex);
        }
    }
}
