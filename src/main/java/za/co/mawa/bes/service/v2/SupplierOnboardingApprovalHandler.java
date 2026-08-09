package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.dto.RolePartnerDto;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.supplier.SupplierOnboardingRequest;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.PartnerViewEntity;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.service.PartnerServiceV2;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SupplierOnboardingApprovalHandler implements ApprovalCompletionHandler {

    private final ObjectMapper objectMapper;
    private final PartnerServiceV2 partnerServiceV2;
    private final AttachmentRepository attachmentRepository;
    private final ObjectProvider<SupplierApprovalService> supplierApprovalServiceProvider;

    @Override
    public ApprovalType supports() {
        return ApprovalType.SUPPLIER_ONBOARDING;
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        try {
            String onboardingRequestId = approvalRequest.getReferenceId();
            SupplierOnboardingRequest onboarding = objectMapper.readValue(
                    approvalRequest.getPayloadJson(),
                    SupplierOnboardingRequest.class
            );
            onboarding.getSupplier().setPartnerRole("SUPPLIER");
            PartnerViewEntity created = partnerServiceV2.create(onboarding.getSupplier());
            boolean hasSupplierRole = partnerServiceV2.getRoles(created.getPartnerId()).stream()
                    .anyMatch(role -> "SUPPLIER".equalsIgnoreCase(role));
            if (!hasSupplierRole) {
                RolePartnerDto supplierRole = new RolePartnerDto();
                supplierRole.setPartner(created.getPartnerId());
                supplierRole.setRole("SUPPLIER");
                partnerServiceV2.addPartnersRole(supplierRole);
            }

            List<AttachmentEntity> documents = attachmentRepository.findByObjectId(onboardingRequestId);
            for (AttachmentEntity document : documents) {
                document.setObjectId(created.getPartnerId());
            }
            attachmentRepository.saveAll(documents);

            onboarding.getBankingDetails().setPartner(created.getPartnerId());
            ApprovalRequestResponse bankingApproval = supplierApprovalServiceProvider.getObject()
                    .submitBankingDetails(
                            created.getPartnerId(),
                            onboarding.getBankingDetails(),
                            approvalRequest.getRequesterId()
                    );

            approvalRequest.setReferenceId(created.getPartnerId());
            approvalRequest.setReferenceNo(created.getPartnerNo());
            approvalRequest.setDescription(
                    "Supplier created after final approval. Banking details submitted separately for approval " +
                    bankingApproval.getReferenceNo() + "."
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Approved supplier could not be created: " + ex.getMessage(), ex);
        }
    }
}
