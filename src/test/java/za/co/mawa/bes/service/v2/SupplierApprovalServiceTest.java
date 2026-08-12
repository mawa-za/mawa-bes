package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.dto.partner.PartnerBankAccountDto;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.PartnerViewEntity;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.service.PartnerBankAccountService;
import za.co.mawa.bes.service.PartnerServiceV2;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierApprovalServiceTest {

    @Test
    void bankingSubmissionUsesMaterialisedPartnerViewForExistingSupplier() throws Exception {
        ApprovalService approvalService = mock(ApprovalService.class);
        PartnerServiceV2 partnerServiceV2 = mock(PartnerServiceV2.class);
        PartnerBankAccountService partnerBankAccountService = mock(PartnerBankAccountService.class);
        AttachmentRepository attachmentRepository = mock(AttachmentRepository.class);
        ReferenceDataValidationService referenceDataValidationService = mock(ReferenceDataValidationService.class);
        UniversalBranchCodeService universalBranchCodeService = mock(UniversalBranchCodeService.class);

        SupplierApprovalService service = new SupplierApprovalService(
                approvalService,
                partnerServiceV2,
                partnerBankAccountService,
                attachmentRepository,
                referenceDataValidationService,
                universalBranchCodeService,
                new ObjectMapper()
        );

        PartnerBankAccountDto banking = new PartnerBankAccountDto();
        banking.setAccountHolder("FNB Supplier");
        banking.setBankName("FNB");
        banking.setAccountNumber("1234567890");
        banking.setAccountType("CURRENT");

        PartnerViewEntity supplier = PartnerViewEntity.builder()
                .partnerId("supplier-1")
                .partnerNo("SUP0001")
                .name1("FNB")
                .partnerRole("SUPPLIER")
                .build();
        ApprovalRequestResponse approvalResponse = ApprovalRequestResponse.builder().id("approval-1").build();

        when(referenceDataValidationService.requireOption("BANK-NAME", "FNB", "Bank name"))
                .thenReturn("FNB");
        when(referenceDataValidationService.requireOption("BANK-ACCOUNT-TYPE", "CURRENT", "Bank account type"))
                .thenReturn("CURRENT");
        when(universalBranchCodeService.resolve("FNB")).thenReturn("250655");
        when(partnerServiceV2.getRoles("supplier-1")).thenReturn(new java.util.ArrayList<>(List.of("SUPPLIER")));
        when(attachmentRepository.findByObjectId("supplier-1")).thenReturn(List.of(mock(AttachmentEntity.class)));
        when(partnerServiceV2.getById("supplier-1")).thenReturn(supplier);
        when(partnerBankAccountService.getBankAccounts("supplier-1")).thenReturn(null);
        when(approvalService.submitForApproval(any())).thenReturn(approvalResponse);

        ApprovalRequestResponse result = service.submitBankingDetails("supplier-1", banking, "user-1");

        assertSame(approvalResponse, result);
        verify(partnerServiceV2).getById("supplier-1");
        verify(partnerServiceV2, never()).get("supplier-1");
    }
}
