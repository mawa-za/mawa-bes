package za.co.mawa.bes.service.v2.claim;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.v2.MembershipClaimEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.exception.ClaimFormGenerationException;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.AttachmentService;
import za.co.mawa.bes.service.CompanyPdfBrandingService;
import za.co.mawa.bes.service.v2.UserTimeZoneService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class ClaimFormGenerationService {
    private static final String DOCUMENT_TYPE = "CLAIM-FORM";
    private static final String OBJECT_TYPE = "claims";
    private static final float LEFT = 46;
    private static final float RIGHT = 549;
    private static final float RGB_SCALE = 255f;

    private final MembershipClaimRepository claimRepository;
    private final MembershipRepository membershipRepository;
    private final PartnerRepository partnerRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final CompanyPdfBrandingService companyPdfBrandingService;
    private final UserTimeZoneService userTimeZoneService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AttachmentEntity generateForSubmittedClaim(String claimId) {
        MembershipClaimEntity claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        AttachmentEntity existing = latestGeneratedClaimForm(claimId);
        if (existing != null) return existing;
        return attachmentService.saveBytes(generatePdf(claim), "pdf", OBJECT_TYPE, claimId, DOCUMENT_TYPE);
    }

    /** Compatibility entry point: never produce the old incomplete funeral-specific draft. */
    @Transactional
    public AttachmentEntity generateForFuneralClaim(String claimId, String claimNo, String claimType,
                                                     String deceasedName, String claimantName, Long amountCents) {
        return generateForSubmittedClaim(claimId);
    }


    /** Save a new version without overwriting the originally issued attachment. */
    @Transactional
    public AttachmentEntity regenerateStoredForm(String claimId) {
        return attachmentService.saveBytes(generatePdf(claimId), "pdf", OBJECT_TYPE, claimId, DOCUMENT_TYPE);
    }

    private AttachmentEntity latestGeneratedClaimForm(String claimId) {
        return attachmentRepository
                .findFirstByObjectIdAndDocumentTypeOrderByUploadDateDescUploadTimeDescIdDesc(
                        claimId, DOCUMENT_TYPE)
                .orElse(null);
    }

    /** Generates a fresh printable form for local or externally-owned funeral claims. */
    public byte[] generateFuneralPdf(String claimNo, String claimType, String membershipNumber,
                                     String deceasedName, String claimantName, Long amountCents,
                                     String dateOfDeath, String causeOfDeath,
                                     String deathCertificateNo, String notes) {
        return renderClaimForm(new ClaimFormData(
                claimNo, claimType, "DRAFT", membershipNumber, "", deceasedName, claimantName,
                dateOfDeath, causeOfDeath, deathCertificateNo, amountCents, notes, Map.of()));
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
        Map<String, String> extra = new LinkedHashMap<>();
        if (claim.getFuneralDetailsJson() != null && !claim.getFuneralDetailsJson().isBlank()) {
            try {
                Map<String, Object> snapshot = objectMapper.readValue(claim.getFuneralDetailsJson(),
                        new TypeReference<Map<String, Object>>() {});
                snapshot.forEach((key, value) -> extra.put(key, value == null ? "" : value.toString()));
            } catch (Exception ex) {
                throw new ClaimFormGenerationException("Invalid saved funeral context for " + claim.getClaimNo(), ex);
            }
        }
        extra.put("claimDate", display(claim.getClaimDate()));
        extra.put("burialDate", display(claim.getBurialDate()));
        extra.put("deceasedType", display(claim.getDeceasedType()));
        extra.put("coveragePlan", claim.getCoveragePlanId());
        extra.put("requested", formatCents(claim.getClaimAmountCents()));
        extra.put("approved", claim.getApprovedAmountCents() == null ? "Not yet approved" : formatCents(claim.getApprovedAmountCents()));
        extra.put("arrearsMonths", display(claim.getArrearsMonths()));
        extra.put("arrearsFine", formatCents(claim.getArrearsFineCents()));
        extra.put("payoutMethod", display(claim.getPayoutMethod()));
        extra.put("settlementMethod", claim.getSettlementMethod());
        extra.put("settlementReference", claim.getSettlementReference());
        extra.put("settledAt", display(claim.getSettledAt()));
        extra.put("accountHolder", claim.getAccountHolderName());
        extra.put("bankName", claim.getBankName());
        extra.put("maskedAccount", maskAccount(claim.getAccountNumber()));
        extra.put("relationship", display(claim.getDeceasedType()));
        extra.put("memberPlan", membership == null ? "" : membership.getPlanId());
        extra.put("memberNo", member == null ? "" : member.getNo());
        extra.put("claimantNo", claimant == null ? "" : claimant.getNo());
        extra.put("supportingDocuments", attachmentRepository.findByObjectId(claim.getId()).stream()
                .map(AttachmentEntity::getDocumentType).filter(Objects::nonNull)
                .filter(type -> !DOCUMENT_TYPE.equalsIgnoreCase(type)).distinct()
                .collect(java.util.stream.Collectors.joining(", ")));
        extra.put("deceasedNo", deceased == null ? "" : deceased.getNo());
        return renderClaimForm(new ClaimFormData(
                claim.getClaimNo(), display(claim.getClaimType()), display(claim.getStatus()),
                membership == null ? "" : membership.getMembershipNo(), partnerName(member),
                firstNonblank(extra.get("deceasedName"), partnerName(deceased)),
                firstNonblank(partnerName(claimant), extra.get("familyRepresentative")),
                firstNonblank(extra.get("dateOfDeath"), display(claim.getDateOfDeath())),
                firstNonblank(claim.getCauseOfDeath(), extra.get("causeOfDeath")),
                firstNonblank(claim.getDeathCertificateNo(), extra.get("deathCertificateNo")),
                claim.getClaimAmountCents(), claim.getNotes(), extra));
    }

    private String maskAccount(String account) {
        if (account == null || account.isBlank()) return "";
        String value = account.trim();
        return "****" + value.substring(Math.max(0, value.length() - 4));
    }

    private String firstNonblank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private String display(Object value) {
        return value == null ? "" : value.toString();
    }

    private byte[] renderClaimForm(ClaimFormData data) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Three pages intentionally: all claim information remains readable, with no truncated sections.
            PDPage first = new PDPage(PDRectangle.A4);
            document.addPage(first);
            try (PDPageContentStream content = new PDPageContentStream(document, first)) {
                float y = drawHeader(document, content, 794, data);
                y = drawSection(content, "CLAIM IDENTIFICATION", y, new String[][]{
                        {"Claim number", data.claimNo}, {"Claim type", data.claimType},
                        {"Claim status", data.status}, {"Claim date", extra(data, "claimDate")},
                        {"Membership number", data.membershipNumber}
                });
                y = drawSection(content, "MEMBERSHIP DETAILS", y, new String[][]{
                        {"Main member", data.memberName}, {"Member reference", extra(data, "memberNo")},
                        {"Coverage plan reference", extra(data, "memberPlan")},
                        {"Benefit / deceased type", extra(data, "deceasedType")}
                });
                y = drawSection(content, "DECEASED DETAILS", y, new String[][]{
                        {"Deceased", data.deceasedName},
                        {"Identity / document no.", firstNonblank(extra(data, "deceasedIdentityNumber"), extra(data, "deceasedNo"))},
                        {"Relationship / cover", extra(data, "relationship")},
                        {"Date of death", data.dateOfDeath}, {"Cause of death", data.causeOfDeath},
                        {"Death certificate no.", data.deathCertificateNo}, {"Burial date", extra(data, "burialDate")}
                });

            }
            PDPage second = new PDPage(PDRectangle.A4);
            document.addPage(second);
            try (PDPageContentStream content = new PDPageContentStream(document, second)) {
                float y = 793;
                write(content, "MEMBERSHIP CLAIM FORM - CONTINUED", LEFT, y, 15, true);
                writeRight(content, clean(data.claimNo), RIGHT, y, 11, true);
                y -= 30;
                y = drawSection(content, "FUNERAL ARRANGEMENT", y, new String[][]{
                        {"FSR reference", extra(data, "serviceRequestNo")},
                        {"Funeral provider tenant", extra(data, "funeralProviderTenantId")},
                        {"Funeral date", extra(data, "funeralDate")},
                        {"Funeral area", extra(data, "funeralArea")},
                        {"Selected package", extra(data, "funeralPackageName")},
                        {"Arrangement total", arrangementTotal(data)}
                });
                y = drawSection(content, "CLAIMANT AND FAMILY REPRESENTATIVE", y, new String[][]{
                        {"Claimant", data.claimantName}, {"Claimant reference", extra(data, "claimantNo")},
                        {"Family representative", extra(data, "familyRepresentative")},
                        {"Representative contact", extra(data, "familyRepresentativeContact")},
                        {"Funeral directions", extra(data, "funeralAddressDirections")}
                });
                y = drawSection(content, "CLAIM FINANCIALS", y, new String[][]{
                        {"Requested amount", formatCents(data.amountCents)},
                        {"Approved amount", extra(data, "approved")},
                        {"Arrears months", extra(data, "arrearsMonths")},
                        {"Arrears fine", extra(data, "arrearsFine")},
                        {"Claim notes", data.notes}
                });
                y = drawSection(content, "PAYMENT AND SETTLEMENT", y, new String[][]{
                        {"Payout method", extra(data, "payoutMethod")},
                        {"Settlement method", extra(data, "settlementMethod")},
                        {"Settlement reference", extra(data, "settlementReference")},
                        {"Settlement date", extra(data, "settledAt")},
                        {"Recipient / account holder", extra(data, "accountHolder")},
                        {"Bank / masked account", (extra(data, "bankName") + " " + extra(data, "maskedAccount")).trim()}
                });
            }
            PDPage third = new PDPage(PDRectangle.A4);
            document.addPage(third);
            try (PDPageContentStream content = new PDPageContentStream(document, third)) {
                float y = 793;
                write(content, "CLAIM SUPPORTING DOCUMENTS AND DECLARATION", LEFT, y, 14, true);
                writeRight(content, clean(data.claimNo), RIGHT, y, 11, true);
                y = drawSection(content, "DOCUMENTS ATTACHED", y - 36, new String[][]{
                        {"Recorded document types", extra(data, "supportingDocuments")}
                });
                y = drawChecklist(content, y);
                drawDeclaration(content, y);
            }
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            String claimReference = clean(data.claimNo);
            throw new ClaimFormGenerationException(
                    claimReference.isBlank() ? "Failed to generate claim form" :
                            "Failed to generate claim form for claim " + claimReference, e);
        }
    }

    private String arrangementTotal(ClaimFormData data) {
        try { return extra(data, "arrangementTotalCents").isBlank() ? ""
                    : formatCents(Long.parseLong(extra(data, "arrangementTotalCents"))); }
        catch (NumberFormatException ex) { return ""; }
    }

    private String extra(ClaimFormData data, String key) {
        if (data.extra == null) return "";
        String value = data.extra.get(key);
        return value == null ? "" : value;
    }

    private float drawHeader(PDDocument document, PDPageContentStream content, float y, ClaimFormData data) throws Exception {
        y = companyPdfBrandingService.drawPdfBoxHeader(document, content, regularFont(), boldFont(), LEFT, RIGHT, y);
        writeRight(content, "Generated " + userTimeZoneService.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")), RIGHT, y + 2, 8, false);
        y -= 24;
        write(content, "MEMBERSHIP CLAIM FORM", LEFT, y, 18, true);
        writeRight(content, clean(data.claimNo), RIGHT, y, 11, true);
        return y - 24;
    }

    private PDFont regularFont() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private PDFont boldFont() {
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    }

    private float drawSection(PDPageContentStream content, String title, float y, String[][] rows) throws Exception {
        float rowHeight = 25;
        float height = 24 + rows.length * rowHeight;
        content.setLineWidth(.8f);
        content.addRect(LEFT, y - height, RIGHT - LEFT, height);
        content.stroke();
        content.setNonStrokingColor(235f / RGB_SCALE, 238f / RGB_SCALE, 242f / RGB_SCALE);
        content.addRect(LEFT, y - 24, RIGHT - LEFT, 24);
        content.fill();
        content.setNonStrokingColor(0f, 0f, 0f);
        write(content, title, LEFT + 9, y - 17, 10, true);
        float rowY = y - 24;
        for (String[] row : rows) {
            content.moveTo(LEFT, rowY - rowHeight); content.lineTo(RIGHT, rowY - rowHeight); content.stroke();
            content.moveTo(LEFT + 170, rowY); content.lineTo(LEFT + 170, rowY - rowHeight); content.stroke();
            write(content, row[0], LEFT + 9, rowY - 17, 9, true);
            write(content, truncate(present(row[1]), 55), LEFT + 180, rowY - 17, 9, false);
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

    private String present(String value) {
        return clean(value).isBlank() ? "Not supplied" : clean(value);
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
                                 Long amountCents, String notes, Map<String, String> extra) {}
}
