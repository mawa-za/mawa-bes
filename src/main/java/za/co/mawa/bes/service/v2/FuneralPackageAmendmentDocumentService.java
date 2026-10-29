package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.FuneralPackageAmendmentEntity;
import za.co.mawa.bes.entity.v2.FuneralServiceEntity;
import za.co.mawa.bes.repository.v2.FuneralPackageAmendmentRepository;
import za.co.mawa.bes.repository.v2.FuneralServiceRepository;
import za.co.mawa.bes.service.CompanyPdfBrandingService;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/** Printed amendment agreement, independent from claim approval histories. */
@Service
@RequiredArgsConstructor
public class FuneralPackageAmendmentDocumentService {
    private final FuneralPackageAmendmentRepository amendments;
    private final FuneralServiceRepository services;
    private final CompanyPdfBrandingService branding;
    private final ObjectMapper json;

    public byte[] generate(String amendmentId) {
        FuneralPackageAmendmentEntity amendment = amendments.findById(amendmentId)
                .orElseThrow(() -> new IllegalArgumentException("Amendment not found"));
        FuneralServiceEntity service = services.findById(amendment.getFuneralServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Funeral arrangement not found"));
        try {
            JsonNode before = json.readTree(amendment.getOldPackageSnapshot());
            JsonNode after = json.readTree(amendment.getNewPackageSnapshot());
            List<String> rows = new ArrayList<>();
            rows.add("FUNERAL SERVICE PACKAGE AMENDMENT");
            rows.add("Amendment: " + amendment.getAmendmentNo());
            rows.add("Funeral service request: " + service.getServiceRequestNo());
            rows.add("Deceased: " + service.getDeceasedName());
            rows.add("Family representative: " + service.getFamilyRepNames() + " " + service.getFamilyRepSurname());
            rows.add("Funeral date: " + service.getFuneralDate());
            rows.add("Status: " + amendment.getStatus());
            rows.add(" ");
            rows.add("ORIGINAL PACKAGE: " + before.path("name").asText("Not recorded"));
            addPackageDetails(rows, before);
            rows.add(" ");
            rows.add("REVISED PACKAGE: " + after.path("name").asText("Not recorded"));
            addPackageDetails(rows, after);
            rows.add(" ");
            rows.add("Original agreed total: " + money(amendment.getOldTotalCents()));
            rows.add("Revised agreed total: " + money(amendment.getNewTotalCents()));
            rows.add("Price difference: " + money(amendment.getNewTotalCents() - amendment.getOldTotalCents()));
            rows.add(" ");
            rows.add("REASON FOR CHANGE");
            rows.add(amendment.getReason());
            rows.add(" ");
            rows.add("Customer / family acknowledgement: " +
                    (Boolean.TRUE.equals(amendment.getCustomerAcknowledged()) ? "Confirmed" : "Not confirmed"));
            rows.add("Requested on: " + amendment.getCreatedAt());
            rows.add("Approved on: " + (amendment.getApprovedAt() == null ? "Pending" : amendment.getApprovedAt()));
            if (amendment.getAdjustmentDocumentsJson() != null) {
                rows.add("Financial adjustments: " + amendment.getAdjustmentDocumentsJson());
            }
            if (amendment.getReconciliationNote() != null) rows.add("Reconciliation: " + amendment.getReconciliationNote());
            rows.add(" ");
            rows.add("This amendment changes the funeral package only after approval and reconciliation.");
            rows.add("The original invoice, claims and earlier agreement remain on record.");

            try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                PDPageContentStream content = null;
                float y = 0;
                try {
                    for (String row : rows) {
                        for (String line : wrap(ascii(row), 95)) {
                            if (content == null || y < 70) {
                                if (content != null) content.close();
                                PDPage page = new PDPage(PDRectangle.A4);
                                document.addPage(page);
                                content = new PDPageContentStream(document, page);
                                y = branding.drawPdfBoxHeader(document, content,
                                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                                        new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                                        48, 548, 794) - 18;
                            }
                            boolean heading = line.startsWith("ORIGINAL PACKAGE:")
                                    || line.startsWith("REVISED PACKAGE:")
                                    || line.equals("REASON FOR CHANGE")
                                    || line.equals("FUNERAL SERVICE PACKAGE AMENDMENT");
                            content.beginText();
                            content.setFont(new PDType1Font(heading
                                    ? Standard14Fonts.FontName.HELVETICA_BOLD
                                    : Standard14Fonts.FontName.HELVETICA), heading ? 11 : 9);
                            content.newLineAtOffset(48, y);
                            content.showText(line.isBlank() ? " " : line);
                            content.endText();
                            y -= heading ? 21 : 15;
                        }
                    }
                } finally {
                    if (content != null) content.close();
                }
                document.save(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate the funeral package amendment document", e);
        }
    }

    private void addPackageDetails(List<String> rows, JsonNode pkg) throws Exception {
        rows.add("Pricing mode: " + pkg.path("pricingMode").asText("Not recorded"));
        rows.add("Package base price: " + money(pkg.path("basePriceCents").asLong(0)));
        JsonNode items = pkg.path("products");
        if (items.isArray()) {
            for (JsonNode item : items) {
                rows.add("  - " + item.path("description").asText("Item")
                        + " x " + item.path("quantity").asText("1"));
            }
        }
        String inclusionsJson = pkg.path("inclusionsJson").asText("");
        if (!inclusionsJson.isBlank()) {
            JsonNode inclusions = json.readTree(inclusionsJson);
            if (inclusions.isArray()) {
                for (JsonNode included : inclusions) rows.add("  - " + included.asText());
            }
        }
    }

    private String money(long cents) { return String.format(java.util.Locale.US, "R %,.2f", cents / 100.0); }
    private String ascii(String value) { return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?"); }
    private List<String> wrap(String raw, int max) {
        List<String> result = new ArrayList<>();
        String remaining = raw.strip();
        if (remaining.isEmpty()) return List.of(" ");
        while (remaining.length() > max) {
            int breakAt = remaining.lastIndexOf(' ', max);
            if (breakAt < max / 2) breakAt = max;
            result.add(remaining.substring(0, breakAt));
            remaining = remaining.substring(breakAt).stripLeading();
        }
        result.add(remaining);
        return result;
    }
}
