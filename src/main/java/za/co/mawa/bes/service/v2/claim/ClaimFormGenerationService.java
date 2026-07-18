package za.co.mawa.bes.service.v2.claim;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.CompanyLogoService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClaimFormGenerationService {
    private static final String DOCUMENT_TYPE = "CLAIM-FORM";
    private static final String OBJECT_TYPE = "claims";

    private final MembershipClaimRepository claimRepository;
    private final MembershipRepository membershipRepository;
    private final PartnerRepository partnerRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final CompanyLogoService companyLogoService;

    @Transactional
    public AttachmentEntity generateForSubmittedClaim(String claimId) {
        MembershipClaimEntity claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));

        AttachmentEntity existing = attachmentRepository.findByObjectDocumentType(claimId, DOCUMENT_TYPE);
        if (existing != null) {
            return existing;
        }

        byte[] pdf = generatePdf(claim);
        return attachmentService.saveBytes(pdf, "pdf", OBJECT_TYPE, claimId, DOCUMENT_TYPE);
    }

    @Transactional
    public AttachmentEntity generateForFuneralClaim(String claimId, String claimNo, String claimType, String deceasedName, String claimantName, Long amountCents) {
        AttachmentEntity existing = attachmentRepository.findByObjectDocumentType(claimId, DOCUMENT_TYPE);
        if (existing != null) return existing;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                y = drawLogoOrPlaceholder(document, content, y);
                write(content, "MAWA MEMBERSHIP CLAIM FORM", 50, y, 18, true); y -= 35;
                y = row(content, "Claim Number", claimNo, y);
                y = row(content, "Claim Type", claimType, y);
                y = row(content, "Deceased", deceasedName, y);
                y = row(content, "Claimant", claimantName, y);
                y = row(content, "Claim Amount", formatCents(amountCents), y); y -= 35;
                write(content, "Claimant Signature: ______________________________", 50, y, 11, false);
                y -= 28; write(content, "Date: __________________", 50, y, 11, false);
            }
            document.save(out);
            return attachmentService.saveBytes(out.toByteArray(), "pdf", OBJECT_TYPE, claimId, DOCUMENT_TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate funeral claim form", e);
        }
    }

    public byte[] generatePdf(String claimId) {
        MembershipClaimEntity claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        return generatePdf(claim);
    }

    private byte[] generatePdf(MembershipClaimEntity claim) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 780;
                y = drawLogoOrPlaceholder(document, content, y);
                write(content, "MAWA MEMBERSHIP CLAIM FORM", 50, y, 18, true);
                y -= 30;
                write(content, "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 50, y, 9, false);
                y -= 28;

                MembershipEntity membership = membershipRepository.findById(claim.getMembershipId()).orElse(null);
                PartnerEntity member = membership == null ? null : partnerRepository.findById(membership.getMemberId()).orElse(null);
                PartnerEntity deceased = partnerRepository.findById(claim.getDeceasedPartnerId()).orElse(null);
                PartnerEntity claimant = Optional.ofNullable(claim.getClaimantPartnerId())
                        .flatMap(partnerRepository::findById)
                        .orElse(null);

                y = section(content, "Claim Details", y);
                y = row(content, "Claim Number", claim.getClaimNo(), y);
                y = row(content, "Claim Type", String.valueOf(claim.getClaimType()), y);
                y = row(content, "Status", String.valueOf(claim.getStatus()), y);
                y = row(content, "Claim Date", String.valueOf(claim.getClaimDate()), y);
                y = row(content, "Date of Death", String.valueOf(claim.getDateOfDeath()), y);
                y = row(content, "Claim Amount", formatCents(claim.getClaimAmountCents()), y);
                y -= 10;

                y = section(content, "Membership", y);
                y = row(content, "Membership Number", membership == null ? "" : membership.getMembershipNo(), y);
                y = row(content, "Membership Status", membership == null ? "" : membership.getStatus(), y);
                y = row(content, "Main Member", partnerName(member), y);
                y -= 10;

                y = section(content, "Deceased", y);
                y = row(content, "Deceased Type", String.valueOf(claim.getDeceasedType()), y);
                y = row(content, "Deceased Name", partnerName(deceased), y);
                y = row(content, "Cause of Death", claim.getCauseOfDeath(), y);
                y = row(content, "Death Certificate No", claim.getDeathCertificateNo(), y);
                y -= 10;

                y = section(content, "Claimant", y);
                y = row(content, "Claimant Name", partnerName(claimant), y);
                y = row(content, "Notes", claim.getNotes(), y);
                y -= 40;

                write(content, "Claimant Signature: ______________________________", 50, y, 11, false);
                y -= 28;
                write(content, "Date: __________________", 50, y, 11, false);
                write(content, "Captured By: __________________", 300, y, 11, false);
            }

            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate claim form", e);
        }
    }

    private float drawLogoOrPlaceholder(PDDocument document, PDPageContentStream content, float y) throws Exception {
        float width = CompanyLogoService.PDF_WIDTH_PT;
        float height = CompanyLogoService.PDF_HEIGHT_PT;
        if (companyLogoService.getActiveLogo().isPresent()) {
            byte[] logoBytes = companyLogoService.getActiveLogo().get().getContent();
            PDImageXObject image = PDImageXObject.createFromByteArray(document, logoBytes, "company-logo");
            content.drawImage(image, 50, y - height + 8, width, height);
        } else {
            content.addRect(50, y - height + 8, width, height);
            content.stroke();
            write(content, "COMPANY LOGO", 78, y - 18, 9, true);
        }
        return y - height - 8;
    }

    private float section(PDPageContentStream content, String title, float y) throws Exception {
        write(content, title, 50, y, 13, true);
        return y - 18;
    }

    private float row(PDPageContentStream content, String label, String value, float y) throws Exception {
        write(content, label + ":", 65, y, 10, true);
        write(content, clean(value), 200, y, 10, false);
        return y - 16;
    }

    private void write(PDPageContentStream content, String text, float x, float y, int size, boolean bold) throws Exception {
        content.beginText();
        content.setFont(new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA), size);
        content.newLineAtOffset(x, y);
        content.showText(truncate(clean(text), 90));
        content.endText();
    }

    private String partnerName(PartnerEntity partner) {
        if (partner == null) return "";
        return clean(partner.getName1()) + " " + clean(partner.getName2()) + " " + clean(partner.getName3());
    }

    private String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private String formatCents(Long cents) {
        long amount = cents == null ? 0L : cents;
        return "R " + String.format("%.2f", amount / 100.0);
    }
}
