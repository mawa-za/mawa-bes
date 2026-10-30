package za.co.mawa.bes.service.v2.claim;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.entity.v2.PaymentRequestEntity;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.UserRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.repository.v2.MembershipPlanRepository;
import za.co.mawa.bes.repository.v2.PaymentRequestRepository;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.CompanyPdfBrandingService;
import za.co.mawa.bes.service.v2.UserTimeZoneService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimFormGenerationServiceTest {
    @Mock MembershipClaimRepository claims;
    @Mock MembershipRepository memberships;
    @Mock PartnerRepository partners;
    @Mock UserRepository users;
    @Mock MembershipPlanRepository plans;
    @Mock PaymentRequestRepository payments;
    @Mock AttachmentRepository attachments;
    @Mock AttachmentService storage;
    @Mock CompanyPdfBrandingService branding;
    @Mock UserTimeZoneService time;
    @Spy ObjectMapper json = new ObjectMapper();
    @InjectMocks ClaimFormGenerationService forms;

    @Test
    void membershipTenantFormUsesFuneralSnapshotAndDoesNotPrintApprovalHistory() throws Exception {
        MembershipClaimEntity claim = new MembershipClaimEntity();
        claim.setId("claim-id");
        claim.setMembershipId("member-1");
        claim.setClaimNo("CLM-000010");
        claim.setClaimType(MembershipClaimType.FUNERAL);
        claim.setStatus(MembershipClaimStatus.APPROVED);
        claim.setDeceasedPartnerId("deceased-1");
        claim.setClaimantPartnerId("claimant-1");
        claim.setDateOfDeath(LocalDate.of(2026, 10, 20));
        claim.setClaimDate(LocalDate.of(2026, 10, 21));
        claim.setClaimAmountCents(1000000L);
        claim.setApprovedAmountCents(1000000L);
        claim.setCreatedBy("capturer-1");
        claim.setUpdatedBy("BGUSER");
        claim.setApprovedBy("capturer-1");
        claim.setApprovedAt(LocalDateTime.of(2026,10,28,11,15));
        claim.setPaymentRequestId("payment-1");
        claim.setFuneralDetailsJson("""
                {"serviceRequestNo":"FSR-123","funeralProviderTenantId":"provider-tenant",
                "funeralProviderName":"Example Funeral Services", "deceasedName":"Jane Deceased", "familyRepresentative":"Family Representative",
                "funeralDate":"2026-10-31", "funeralArea":"Ga-Makapan", "funeralPackageName":"Gold",
                "deceasedIdentityNumber":"1234567890", "familyRepresentativeContact":"012 345 6789"}
                """);
        MembershipEntity membership = new MembershipEntity();
        membership.setId("member-1"); membership.setMemberId("holder-1");
        membership.setMembershipNo("MEM-001");
        PartnerEntity holder = new PartnerEntity();
        holder.setName2("Main"); holder.setName1("Member");
        when(payments.findById("payment-1")).thenReturn(Optional.of(PaymentRequestEntity.builder()
                .payeeName("Funeral supplier").paymentMethod(PaymentMethod.EFT)
                .paidReference("FNB-REF-123").paidDate(LocalDate.of(2026,10,29))
                .bankName("FNB").accountNumber("1234567890").build()));
        when(claims.findById("claim-id")).thenReturn(Optional.of(claim));
        when(memberships.findById("member-1")).thenReturn(Optional.of(membership));
        when(partners.findById("holder-1")).thenReturn(Optional.of(holder));
        when(partners.findById("deceased-1")).thenReturn(Optional.empty());
        when(partners.findById("claimant-1")).thenReturn(Optional.empty());
        when(attachments.findByObjectId("claim-id")).thenReturn(List.of());
        when(users.findById("capturer-1")).thenReturn(Optional.of(
                UserEntity.builder().id("capturer-1").username("Funeral processor").email("processor@mawa.test").build()));
        when(branding.drawPdfBoxHeader(any(PDDocument.class), any(PDPageContentStream.class),
                any(PDFont.class), any(PDFont.class), anyFloat(), anyFloat(), anyFloat())).thenReturn(735f);
        when(time.now()).thenReturn(LocalDateTime.of(2026, 10, 29, 12, 0));

        byte[] pdf = forms.generatePdf("claim-id");
        assertEquals('%', (char) pdf[0]);
        try (PDDocument parsed = Loader.loadPDF(pdf)) {
            assertEquals(3, parsed.getNumberOfPages());
            String text = new PDFTextStripper().getText(parsed);
            assertTrue(text.contains("FSR-123"));
            assertTrue(text.contains("Jane Deceased"));
            assertTrue(text.contains("Ga-Makapan"));
            assertTrue(text.contains("Gold"));
            assertTrue(text.contains("MEM-001"));
            assertTrue(text.contains("FUNERAL ARRANGEMENT"));
            assertTrue(text.contains("Example Funeral Services"));
            assertTrue(text.contains("FNB-REF-123"));
            assertTrue(text.contains("****7890"));
            assertFalse(text.contains("BGUSER"));
            assertTrue(text.contains("CLAIM PROCESSING DETAILS"));
            assertTrue(text.contains("Funeral processor"));
            assertTrue(text.contains("processor@mawa.test"));
            assertFalse(text.toUpperCase().contains("CLAIM STATUS"));
            assertFalse(text.toUpperCase().contains("APPROVAL HISTORY"));
        }
    }

    @Test
    void cashClaimOmitsFuneralArrangementAndFamilyRepresentative() throws Exception {
        MembershipClaimEntity claim = new MembershipClaimEntity();
        claim.setId("cash-claim");
        claim.setMembershipId("member-1");
        claim.setClaimNo("CASH-0001");
        claim.setClaimType(MembershipClaimType.CASH);
        claim.setStatus(MembershipClaimStatus.DRAFT);
        claim.setDeceasedPartnerId("deceased-1");
        claim.setDateOfDeath(LocalDate.of(2026, 10, 20));
        claim.setClaimDate(LocalDate.of(2026, 10, 21));
        claim.setClaimAmountCents(190000L);
        MembershipEntity membership = new MembershipEntity();
        membership.setId("member-1");
        membership.setMemberId("holder-1");
        membership.setMembershipNo("MEM-002");
        when(claims.findById("cash-claim")).thenReturn(Optional.of(claim));
        when(memberships.findById("member-1")).thenReturn(Optional.of(membership));
        when(partners.findById("holder-1")).thenReturn(Optional.empty());
        when(partners.findById("deceased-1")).thenReturn(Optional.empty());
        when(attachments.findByObjectId("cash-claim")).thenReturn(List.of());
        when(branding.drawPdfBoxHeader(any(PDDocument.class), any(PDPageContentStream.class),
                any(PDFont.class), any(PDFont.class), anyFloat(), anyFloat(), anyFloat())).thenReturn(735f);
        when(time.now()).thenReturn(LocalDateTime.of(2026, 10, 29, 12, 0));

        try (PDDocument parsed = Loader.loadPDF(forms.generatePdf("cash-claim"))) {
            String text = new PDFTextStripper().getText(parsed);
            assertTrue(text.contains("CLAIMANT DETAILS"));
            assertTrue(text.contains("CLAIM PROCESSING DETAILS"));
            assertFalse(text.contains("FUNERAL ARRANGEMENT"));
            assertFalse(text.contains("FAMILY REPRESENTATIVE"));
            assertFalse(text.toUpperCase().contains("CLAIM STATUS"));
        }
    }

    @Test
    void combinationClaimIncludesFuneralArrangement() throws Exception {
        MembershipClaimEntity claim = new MembershipClaimEntity();
        claim.setId("combo-claim");
        claim.setMembershipId("member-1");
        claim.setClaimNo("COMBO-0001");
        claim.setClaimType(MembershipClaimType.COMBINATION);
        claim.setDeceasedPartnerId("deceased-1");
        claim.setDateOfDeath(LocalDate.of(2026, 10, 20));
        claim.setClaimAmountCents(2500000L);
        claim.setFuneralDetailsJson("{\"serviceRequestNo\":\"FSR-321\"}");
        MembershipEntity membership = new MembershipEntity();
        membership.setId("member-1");
        membership.setMemberId("holder-1");
        when(claims.findById("combo-claim")).thenReturn(Optional.of(claim));
        when(memberships.findById("member-1")).thenReturn(Optional.of(membership));
        when(partners.findById("holder-1")).thenReturn(Optional.empty());
        when(partners.findById("deceased-1")).thenReturn(Optional.empty());
        when(attachments.findByObjectId("combo-claim")).thenReturn(List.of());
        when(branding.drawPdfBoxHeader(any(PDDocument.class), any(PDPageContentStream.class),
                any(PDFont.class), any(PDFont.class), anyFloat(), anyFloat(), anyFloat())).thenReturn(735f);
        when(time.now()).thenReturn(LocalDateTime.of(2026, 10, 29, 12, 0));

        try (PDDocument parsed = Loader.loadPDF(forms.generatePdf("combo-claim"))) {
            String text = new PDFTextStripper().getText(parsed);
            assertTrue(text.contains("FUNERAL ARRANGEMENT"));
            assertTrue(text.contains("FSR-321"));
        }
    }

}
