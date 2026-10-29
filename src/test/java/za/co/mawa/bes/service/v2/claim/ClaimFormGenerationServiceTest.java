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
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
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
        claim.setFuneralDetailsJson("""
                {"serviceRequestNo":"FSR-123","funeralProviderTenantId":"provider-tenant",
                "deceasedName":"Jane Deceased", "familyRepresentative":"Family Representative",
                "funeralDate":"2026-10-31", "funeralArea":"Ga-Makapan", "funeralPackageName":"Gold",
                "deceasedIdentityNumber":"1234567890", "familyRepresentativeContact":"012 345 6789"}
                """);
        MembershipEntity membership = new MembershipEntity();
        membership.setId("member-1"); membership.setMemberId("holder-1");
        membership.setMembershipNo("MEM-001");
        PartnerEntity holder = new PartnerEntity();
        holder.setName2("Main"); holder.setName1("Member");
        when(claims.findById("claim-id")).thenReturn(Optional.of(claim));
        when(memberships.findById("member-1")).thenReturn(Optional.of(membership));
        when(partners.findById("holder-1")).thenReturn(Optional.of(holder));
        when(partners.findById("deceased-1")).thenReturn(Optional.empty());
        when(partners.findById("claimant-1")).thenReturn(Optional.empty());
        when(attachments.findByObjectId("claim-id")).thenReturn(List.of());
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
            assertFalse(text.toUpperCase().contains("APPROVAL HISTORY"));
        }
    }
}
