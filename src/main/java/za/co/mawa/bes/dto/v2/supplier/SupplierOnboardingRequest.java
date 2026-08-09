package za.co.mawa.bes.dto.v2.supplier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.dto.partner.PartnerInboundDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOnboardingRequest {
    private String onboardingRequestId;
    private PartnerInboundDto supplier;
    private PartnerBankAccountDto bankingDetails;
    private Boolean supportingDocumentsComplete;
}
