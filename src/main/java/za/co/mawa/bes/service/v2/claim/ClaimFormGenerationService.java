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
import za.co.mawa.bes.service.CompanyInfoService;
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
    private static final float LEFT = 46;
    private static final float RIGHT = 549;

    private final MembershipClaimRepository claimRepository;
    private final MembershipRepository membershipRepository;
    private final PartnerRepository partnerRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final CompanyLogoService companyLogoService;
    private final CompanyInfoService companyInfoService;

    @Transactional
    public AttachmentEntity generateForSubmittedClaim(String claimId) {
        MembershipClaimEntity claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        AttachmentEntity existing = attachmentRepository.findByObjectDocumentType(claimId, DOCUMENT_TYPE);
        if (existing != null) return existing;
        return attachmentService.saveBytes(generatePdf(claim), "pdf", OBJECT_TYPE, claimId, DOCUMENT_TYPE);
    }

    @Transactional
    public AttachmentEntity generateForFuneralClaim(String claimId, String claimNo, String claimType,
                                                     String deceasedName, String claimantName, Long amountCents) {
        AttachmentEntity existing = attachmentRepository.findByObjectDocumentType(claimId, DOCUMENT_TYPE);
        if (existing != null) return existing;
        byte[] pdf = generateFuneralPdf(claimNo, claimType, "", deceasedName, claimantName, amountCents,
                "", "", "", "");
        return attachmentService.saveBytes(pdf, "pdf", OBJECT_TYPE, claimId, DOCUMENT_TYPE);
    }

    /** Generates a fresh printable form for local or externally-owned funeral claims. */
    public byte[] generateFuneralPdf(String claimNo, String claimType, String membershipNumber,
                                     String deceasedName, String claimantName, Long amountCents,
                                     String dateOfDeath, String causeOfDeath,
                                     String deathCertificateNo, String notes) {
        return renderClaimForm(new ClaimFormData(
                claimNo, claimType, "DRAFT", membershipNumber, "", deceasedName, claimantName,
                dateOfDeath, causeOfDeath, deathCertificateNo, amountCents, notes));
    }

    public byte[] generatePdf(String claimId) {
        MembershipClaimEntity claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        return generatePdf(claim);
    }

    private byte[] generatePdf(MembershipClaimEntity claim) {
        MembershipEntity membership = membershipRepository.findById(claim.getMembershipId()).orElse(null);
        PartnerEntity member = membership == null ? null : partnerRepository.findById(membership.getMemberId()).orElse(null);
        PartnerEntity deceased = partnerRepository.findById(claim.getDeceasedPartnerId()).orElse(null);
        PartnerEntity claimant = Optional.ofNullable(claim.getClaimantPartnerId()).flatMap(partnerRepository::findById).orElse(null);
        return renderClaimForm(new ClaimFormData(
                claim.getClaimNo(), String.valueOf(claim.getClaimType()), String.valueOf(claim.getStatus()),
                membership == null ? "" : membership.getMembershipNo(), partnerName(member), partnerName(deceased),
                partnerName(claimant), String.valueOf(claim.getDateOfDeath()), claim.getCauseOfDeath(),
                claim.getDeathCertificateNo(), claim.getClaimAmountCents(), claim.getNotes()));
    }

    private byte[] renderClaimForm(ClaimFormData data) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 794;
                y = drawHeader(document, content, y, data);
                y = drawSection(content, "CLAIM DETAILS", y,
                        new String[][]{
                                {"Claim number", data.claimNo}, {"Claim type", data.claimType},
                                {"Membership number", data.membershipNumber}, {"Claim amount", formatCents(data.amountCents)}
                        });
                y = drawSection(content, "MEMBER AND DECEASED", y,
                        new String[][]{
                                {"Main member", data.memberName}, {"Deceased", data.deceasedName},
                                {"Date of death", data.dateOfDeath}, {"Cause of death", data.causeOfDeath},
                                {"Death certificate no.", data.deathCertificateNo}, {"Claimant", data.claimantName}
                        });
                y = drawDeclaration(content, y);
                y = drawChecklist(content, y);
                drawOfficeUse(content, y, data);
            }
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate claim form", e);
        }
    }

    private float drawHeader(PDDocument document, PDPageContentStream content, float y, ClaimFormData data) throws Exception {
        float logoWidth = CompanyLogoService.PDF_WIDTH_PT;
        float logoHeight = CompanyLogoService.PDF_HEIGHT_PT;
        if (companyLogoService.getActiveLogo().isPresent()) {
            PDImageXObject image = PDImageXObject.createFromByteArray(document,
                    companyLogoService.getActiveLogo().get().getContent(), "company-logo");
            content.drawImage(image, LEFT, y - logoHeight, logoWidth, logoHeight);
        } else {
            content.addRect(LEFT, y - logoHeight, logoWidth, logoHeight);
            content.stroke();
            write(content, "COMPANY LOGO", LEFT + 25, y - 25, 9, true);
        }
        writeRight(content, clean(companyInfoService.getCompanyName()), RIGHT, y - 10, 14, true);
        writeRight(content, clean(companyInfoService.getCompanyAddress()), RIGHT, y - 27, 8, false);
        writeRight(content, "Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")), RIGHT, y - 43, 8, false);
        y -= logoHeight + 16;
        content.setLineWidth(1.2f);
        content.moveTo(LEFT, y); content.lineTo(RIGHT, y); content.stroke();
        y -= 28;
        write(content, "MEMBERSHIP CLAIM FORM", LEFT, y, 18, true);
        writeRight(content, clean(data.claimNo), RIGHT, y, 11, true);
        return y - 24;
    }

    private float drawSection(PDPageContentStream content, String title, float y, String[][] rows) throws Exception {
        float rowHeight = 25;
        float height = 24 + rows.length * rowHeight;
        content.setLineWidth(.8f);
        content.addRect(LEFT, y - height, RIGHT - LEFT, height);
        content.stroke();
        content.setNonStrokingColor(235, 238, 242);
        content.addRect(LEFT, y - 24, RIGHT - LEFT, 24);
        content.fill();
        content.setNonStrokingColor(0, 0, 0);
        write(content, title, LEFT + 9, y - 17, 10, true);
        float rowY = y - 24;
        for (String[] row : rows) {
            content.moveTo(LEFT, rowY - rowHeight); content.lineTo(RIGHT, rowY - rowHeight); content.stroke();
            content.moveTo(LEFT + 170, rowY); content.lineTo(LEFT + 170, rowY - rowHeight); content.stroke();
            write(content, row[0], LEFT + 9, rowY - 17, 9, true);
            write(content, truncate(clean(row[1]), 65), LEFT + 180, rowY - 17, 9, false);
            rowY -= rowHeight;
        }
        return y - height - 13;
    }

    private float drawDeclaration(PDPageContentStream content, float y) throws Exception {
        float height = 116;
        content.addRect(LEFT, y - height, RIGHT - LEFT, height); content.stroke();
        write(content, "CLAIMANT DECLARATION", LEFT + 9, y - 18, 10, true);
        write(content, "I declare that the information supplied in this claim and the supporting documents is true and complete.", LEFT + 9, y - 40, 9, false);
        write(content, "I authorise verification of the information where required for assessment of this claim.", LEFT + 9, y - 57, 9, false);
        write(content, "Claimant signature", LEFT + 9, y - 88, 9, true);
        content.moveTo(LEFT + 112, y - 90); content.lineTo(LEFT + 322, y - 90); content.stroke();
        write(content, "Date", LEFT + 345, y - 88, 9, true);
        content.moveTo(LEFT + 378, y - 90); content.lineTo(RIGHT - 10, y - 90); content.stroke();
        return y - height - 13;
    }

    private float drawChecklist(PDPageContentStream content, float y) throws Exception {
        float height = 82;
        content.addRect(LEFT, y - height, RIGHT - LEFT, height); content.stroke();
        write(content, "SUPPORTING DOCUMENT CHECKLIST", LEFT + 9, y - 18, 10, true);
        write(content, "[  ] Signed claim form", LEFT + 12, y - 40, 9, false);
        write(content, "[  ] Death certificate / notice of death", LEFT + 190, y - 40, 9, false);
        write(content, "[  ] Identity documents", LEFT + 12, y - 60, 9, false);
        write(content, "[  ] Other required claim evidence", LEFT + 190, y - 60, 9, false);
        return y - height - 13;
    }

    private void drawOfficeUse(PDPageContentStream content, float y, ClaimFormData data) throws Exception {
        float height = 76;
        content.addRect(LEFT, y - height, RIGHT - LEFT, height); content.stroke();
        write(content, "OFFICE USE", LEFT + 9, y - 18, 10, true);
        write(content, "Received by", LEFT + 9, y - 43, 9, true);
        content.moveTo(LEFT + 82, y - 45); content.lineTo(LEFT + 235, y - 45); content.stroke();
        write(content, "Date", LEFT + 255, y - 43, 9, true);
        content.moveTo(LEFT + 286, y - 45); content.lineTo(LEFT + 380, y - 45); content.stroke();
        write(content, "Status", LEFT + 397, y - 43, 9, true);
        write(content, clean(data.status), LEFT + 440, y - 43, 9, false);
    }

    private void write(PDPageContentStream content, String text, float x, float y, int size, boolean bold) throws Exception {
        content.beginText();
        content.setFont(font(bold), size);
        content.newLineAtOffset(x, y);
        content.showText(clean(text));
        content.endText();
    }

    private void writeRight(PDPageContentStream content, String text, float right, float y, int size, boolean bold) throws Exception {
        String value = truncate(clean(text), 72);
        float width = font(bold).getStringWidth(value) / 1000f * size;
        write(content, value, Math.max(LEFT, right - width), y, size, bold);
    }

    private PDType1Font font(boolean bold) {
        return new PDType1Font(bold ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
    }

    private String partnerName(PartnerEntity partner) {
        if (partner == null) return "";
        return (clean(partner.getName2()) + " " + clean(partner.getName3()) + " " + clean(partner.getName1())).trim();
    }

    private String clean(String value) {
        return value == null || "null".equalsIgnoreCase(value) ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String formatCents(Long cents) {
        long amount = cents == null ? 0L : cents;
        return "R " + String.format("%,.2f", amount / 100.0);
    }

    private record ClaimFormData(String claimNo, String claimType, String status, String membershipNumber,
                                 String memberName, String deceasedName, String claimantName,
                                 String dateOfDeath, String causeOfDeath, String deathCertificateNo,
                                 Long amountCents, String notes) {}
}
