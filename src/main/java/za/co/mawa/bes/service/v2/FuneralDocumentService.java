package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.funeral.FuneralExtraDto;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.v2.FuneralPackageEntity;
import za.co.mawa.bes.entity.v2.FuneralServiceEntity;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.FuneralPackageRepository;
import za.co.mawa.bes.repository.v2.FuneralServiceRepository;
import za.co.mawa.bes.service.CompanyInfoService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FuneralDocumentService {

    private final FuneralServiceRepository funeralServiceRepository;
    private final FuneralPackageRepository funeralPackageRepository;
    private final PartnerRepository partnerRepository;
    private final CompanyInfoService companyInfoService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public byte[] generateConfirmationLetter(String funeralServiceId) {
        FuneralServiceEntity service = getService(funeralServiceId);
        PartnerEntity familyRepresentative = hasText(service.getFamilyRepId()) ? partnerRepository.findById(service.getFamilyRepId()).orElse(null) : null;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document document = new Document(pdf)) {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            addCompanyHeader(document, regular, bold);
            document.add(new Paragraph(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                    .setFont(regular).setFontSize(10).setMarginTop(14));
            document.add(new Paragraph("TO WHOM IT MAY CONCERN")
                    .setFont(bold).setFontSize(12).setMarginTop(18).setMarginBottom(18));
            document.add(new Paragraph("CONFIRMATION OF DECEASED IN OUR CARE AND FUNERAL SERVICE")
                    .setFont(bold).setFontSize(13).setTextAlignment(TextAlignment.CENTER).setMarginBottom(18));

            String deceased = value(service.getDeceasedName(), "the deceased");
            String identity = value(service.getDeceasedIdentityNumber(), "");
            String requestNo = value(service.getServiceRequestNo(), service.getId());
            String company = value(companyInfoService.getCompanyName(), "our funeral service");

            StringBuilder first = new StringBuilder("This letter confirms that ")
                    .append(deceased);
            if (!identity.isBlank()) first.append(" (ID/Passport: ").append(identity).append(")");
            first.append(" is currently in the care and storage of ").append(company)
                    .append(" under Funeral Service Request ").append(requestNo).append(".");
            document.add(body(first.toString(), regular));

            StringBuilder second = new StringBuilder(company)
                    .append(" has been appointed to conduct the funeral service and burial arrangements");
            if (service.getFuneralDate() != null) {
                second.append(" scheduled for ")
                        .append(service.getFuneralDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
            }
            if (hasText(service.getFuneralArea())) {
                second.append(" in ").append(service.getFuneralArea().trim());
            }
            second.append(".");
            document.add(body(second.toString(), regular));

            String representativeName = familyRepresentativeName(service, familyRepresentative);
            if (hasText(representativeName)) {
                document.add(body("The recorded family representative for these arrangements is "
                        + representativeName + ".", regular));
            }

            document.add(body("This confirmation is issued at the request of the family for administrative and supporting purposes.", regular));
            document.add(new Paragraph("Yours faithfully,")
                    .setFont(regular).setFontSize(10).setMarginTop(26));
            document.add(new Paragraph(value(companyInfoService.getCompanyName(), "Authorised Funeral Service Provider"))
                    .setFont(bold).setFontSize(10).setMarginTop(28));
            document.add(new Paragraph("Authorised representative: ______________________________")
                    .setFont(regular).setFontSize(10).setMarginTop(10));
            document.add(new Paragraph("Signature: _____________________________________________")
                    .setFont(regular).setFontSize(10).setMarginTop(10));

            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate funeral confirmation letter", exception);
        }
    }

    public byte[] generateServiceRequestForm(String funeralServiceId) {
        FuneralServiceEntity service = getService(funeralServiceId);
        FuneralPackageEntity funeralPackage = hasText(service.getPackageId())
                ? funeralPackageRepository.findById(service.getPackageId()).orElse(null)
                : null;
        PartnerEntity familyRepresentative = hasText(service.getFamilyRepId()) ? partnerRepository.findById(service.getFamilyRepId()).orElse(null) : null;
        List<FuneralExtraDto> extras = parseExtras(service.getExtrasJson());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document document = new Document(pdf)) {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            addCompanyHeader(document, regular, bold);
            document.add(new Paragraph("FUNERAL SERVICE REQUEST FORM")
                    .setFont(bold).setFontSize(17).setTextAlignment(TextAlignment.CENTER).setMarginTop(16).setMarginBottom(14));

            addSection(document, "REQUEST DETAILS", regular, bold, new String[][]{
                    {"Service request number", value(service.getServiceRequestNo(), service.getId())},
                    {"Status", value(service.getStatus(), "")},
                    {"Created", service.getCreatedAt() == null ? "" : service.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))}
            });

            addSection(document, "DECEASED DETAILS", regular, bold, new String[][]{
                    {"Deceased name", value(service.getDeceasedName(), "")},
                    {"ID / Passport", value(service.getDeceasedIdentityNumber(), "")},
                    {"Date of death", service.getDateOfDeath() == null ? "" : service.getDateOfDeath().toString()},
                    {"Death certificate no.", value(service.getDeathCertificateNo(), "")},
                    {"Cause of death", value(service.getCauseOfDeath(), "")},
                    {"Mortuary reference", value(service.getMortuaryInventoryId(), "")}
            });

            String familyName = familyRepresentativeName(service, familyRepresentative);
            String familyNumber = familyRepresentative == null ? "" : value(familyRepresentative.getNo(), "");
            String familyContact = hasText(service.getFamilyRepContactDetails())
                    ? service.getFamilyRepContactDetails().trim()
                    : familyRepresentative == null ? "" : preferredContact(familyRepresentative.getId());
            addSection(document, "FAMILY REPRESENTATIVE", regular, bold, new String[][]{
                    {"Name", familyName},
                    {"Partner number", familyNumber},
                    {"Contact details", familyContact}
            });

            addSection(document, "FUNERAL SERVICE DETAILS", regular, bold, new String[][]{
                    {"Funeral date", service.getFuneralDate() == null ? "" : service.getFuneralDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))},
                    {"Delivery location / area", value(service.getFuneralArea(), "")},
                    {"Directions to deceased delivery location", value(service.getDeceasedDeliveryDirections(), "")}
            });

            addSection(document, "PACKAGE", regular, bold, new String[][]{
                    {"Package", funeralPackage == null ? value(service.getPackageId(), "") : value(funeralPackage.getName(), "")},
                    {"Package amount", funeralPackage == null ? "" : formatCents(funeralPackage.getBasePriceCents())}
            });

            if (!extras.isEmpty()) {
                Table extraTable = new Table(UnitValue.createPercentArray(new float[]{75, 25})).useAllAvailableWidth().setMarginBottom(12);
                extraTable.addHeaderCell(headerCell("EXTRAS", bold));
                extraTable.addHeaderCell(headerCell("AMOUNT", bold));
                for (FuneralExtraDto extra : extras) {
                    extraTable.addCell(detailCell(value(extra.getDescription(), ""), regular));
                    extraTable.addCell(detailCell(formatCents(extra.getAmountCents()), regular).setTextAlignment(TextAlignment.RIGHT));
                }
                document.add(extraTable);
            }

            Table total = new Table(UnitValue.createPercentArray(new float[]{75, 25})).useAllAvailableWidth().setMarginTop(8);
            total.addCell(new Cell().setBorder(Border.NO_BORDER)
                    .add(new Paragraph("TOTAL FUNERAL ARRANGEMENT").setFont(bold).setFontSize(10)));
            total.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph(formatCents(service.getTotalAmountCents())).setFont(bold).setFontSize(11)));
            document.add(total);

            document.add(new Paragraph("AUTHORISATION")
                    .setFont(bold).setFontSize(10).setMarginTop(24).setMarginBottom(12));
            document.add(new Paragraph("Family representative signature: ____________________________________    Date: ______________")
                    .setFont(regular).setFontSize(9).setMarginBottom(14));
            document.add(new Paragraph("Funeral consultant: ________________________________________________    Date: ______________")
                    .setFont(regular).setFontSize(9));

            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate funeral service request form", exception);
        }
    }

    private FuneralServiceEntity getService(String id) {
        return funeralServiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funeral service request not found: " + id));
    }

    private void addCompanyHeader(Document document, PdfFont regular, PdfFont bold) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{62, 38})).useAllAvailableWidth();
        header.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(value(companyInfoService.getCompanyName(), "FUNERAL SERVICE PROVIDER"))
                        .setFont(bold).setFontSize(14))
                .add(new Paragraph(value(companyInfoService.getCompanyAddress(), ""))
                        .setFont(regular).setFontSize(8)));
        header.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(value(companyInfoService.getContactDetails(), ""))
                        .setFont(regular).setFontSize(8)));
        document.add(header);
    }

    private Paragraph body(String text, PdfFont font) {
        return new Paragraph(text).setFont(font).setFontSize(10).setFixedLeading(15).setTextAlignment(TextAlignment.JUSTIFIED).setMarginBottom(12);
    }

    private void addSection(Document document, String title, PdfFont regular, PdfFont bold, String[][] rows) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{34, 66})).useAllAvailableWidth().setMarginBottom(12);
        Cell titleCell = new Cell(1, 2).add(new Paragraph(title).setFont(bold).setFontSize(9));
        titleCell.setBorder(new SolidBorder(0.7f));
        table.addHeaderCell(titleCell);
        for (String[] row : rows) {
            table.addCell(detailCell(row[0], bold));
            table.addCell(detailCell(value(row[1], ""), regular));
        }
        document.add(table);
    }

    private Cell headerCell(String text, PdfFont font) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(9));
    }

    private Cell detailCell(String text, PdfFont font) {
        return new Cell().setPadding(5).add(new Paragraph(value(text, "")).setFont(font).setFontSize(8.5f));
    }

    private List<FuneralExtraDto> parseExtras(String json) {
        if (!hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<FuneralExtraDto>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String preferredContact(String partnerId) {
        if (!hasText(partnerId)) return "";
        List<String> contacts = jdbcTemplate.query(
                "SELECT value FROM partner_contact WHERE partner = ? " +
                        "ORDER BY CASE WHEN UPPER(type) IN ('CELL','CELLPHONE','MOBILE','PHONE') THEN 0 ELSE 1 END, type LIMIT 1",
                (rs, rowNum) -> rs.getString(1), partnerId);
        return contacts.isEmpty() ? "" : value(contacts.get(0), "");
    }

    private String familyRepresentativeName(FuneralServiceEntity service, PartnerEntity legacyPartner) {
        String names = value(service.getFamilyRepNames(), "").trim();
        String surname = value(service.getFamilyRepSurname(), "").trim();
        String typed = (names + " " + surname).trim();
        if (!typed.isEmpty()) return typed;
        return legacyPartner == null ? "" : partnerName(legacyPartner);
    }

    private String partnerName(PartnerEntity partner) {
        if (partner == null) return "";
        return (value(partner.getName2(), "") + " " + value(partner.getName3(), "") + " " + value(partner.getName1(), ""))
                .trim().replaceAll("\\s+", " ");
    }

    private String formatCents(Long cents) {
        long amount = cents == null ? 0L : cents;
        return "R " + String.format(Locale.US, "%,.2f", amount / 100.0);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String value(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }
}
