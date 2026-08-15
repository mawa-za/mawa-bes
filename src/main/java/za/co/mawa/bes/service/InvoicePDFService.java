package za.co.mawa.bes.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.AddressEntity;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.v2.FuneralServiceInvoiceEntity;
import za.co.mawa.bes.entity.v2.company.CompanyLogoEntity;
import za.co.mawa.bes.repository.AddressRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.FuneralServiceInvoiceRepository;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class InvoicePDFService {
    private static final DeviceRgb HEADER_GREY = new DeviceRgb(238, 240, 243);
    private static final DeviceRgb TEXT_GREY = new DeviceRgb(75, 82, 92);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final CompanyInfoService companyInfoService;
    private final CompanyLogoService companyLogoService;
    private final PartnerRepository partnerRepository;
    private final AddressRepository addressRepository;
    private final SettingService settingService;
    private final FuneralServiceInvoiceRepository funeralServiceInvoiceRepository;

    public InvoicePDFService(CompanyInfoService companyInfoService,
                             CompanyLogoService companyLogoService,
                             PartnerRepository partnerRepository,
                             AddressRepository addressRepository,
                             SettingService settingService,
                             FuneralServiceInvoiceRepository funeralServiceInvoiceRepository) {
        this.companyInfoService = companyInfoService;
        this.companyLogoService = companyLogoService;
        this.partnerRepository = partnerRepository;
        this.addressRepository = addressRepository;
        this.settingService = settingService;
        this.funeralServiceInvoiceRepository = funeralServiceInvoiceRepository;
    }

    public ByteArrayOutputStream generateInvoicePdf(InvoiceEntity invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document document = new Document(pdf)) {
            document.setMargins(28, 34, 30, 34);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            addHeader(document, invoice, bold, regular);
            addRecipientAndMetadata(document, invoice, bold, regular);
            addFuneralIdentitySection(document, invoice, bold, regular);
            addLineItems(document, invoice, bold, regular);
            addTotals(document, invoice, bold, regular);
            addPaymentInformation(document, invoice, bold, regular);
            addFooter(document, regular);
        } catch (Exception exception) {
            throw new RuntimeException("Error while creating invoice PDF: " + exception.getMessage(), exception);
        }
        return out;
    }

    private void addHeader(Document document, InvoiceEntity invoice, PdfFont bold, PdfFont regular) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{31, 29, 40})).useAllAvailableWidth();

        Cell logoCell = noBorderCell().setVerticalAlignment(VerticalAlignment.TOP);
        Optional<CompanyLogoEntity> logo = companyLogoService.getActiveLogo();
        if (logo.isPresent()) {
            logoCell.add(new Image(ImageDataFactory.create(logo.get().getContent()))
                    .scaleToFit(CompanyLogoService.PDF_WIDTH_PT, CompanyLogoService.PDF_HEIGHT_PT));
        } else {
            logoCell.add(new Paragraph("COMPANY\nLOGO").setFont(bold).setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER).setBorder(new SolidBorder(1)).setPadding(12));
        }
        header.addCell(logoCell);

        Cell title = noBorderCell().setVerticalAlignment(VerticalAlignment.TOP);
        title.add(new Paragraph("TAX INVOICE").setFont(bold).setFontSize(20).setMarginBottom(7));
        title.add(labelValue("Invoice Date", date(invoice.getInvoiceDate()), bold, regular));
        title.add(labelValue("Invoice Number", safe(invoice.getInvoiceNo()), bold, regular));
        title.add(labelValue("Reference", safe(invoice.getExternalRef()), bold, regular));
        header.addCell(title);

        Cell company = noBorderCell().setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.TOP);
        company.add(new Paragraph(blankDefault(companyInfoService.getCompanyName(), "Company Name"))
                .setFont(bold).setFontSize(13).setMarginBottom(4));
        addRightLine(company, companyInfoService.getCompanyAddress(), regular);
        addRightLine(company, prefixed("Reg No: ", companyInfoService.getCompanyRegistrationNumber()), regular);
        addRightLine(company, prefixed("VAT No: ", companyInfoService.getVATNumber()), regular);
        addRightLine(company, prefixed("FSP No: ", companyInfoService.getFspNumber()), regular);
        addRightLine(company, companyInfoService.getContactDetails(), regular);
        header.addCell(company);
        document.add(header);
        document.add(new Paragraph("").setMarginBottom(2));
    }

    private void addRecipientAndMetadata(Document document, InvoiceEntity invoice, PdfFont bold, PdfFont regular) {
        Table section = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
        Cell recipient = noBorderCell();
        recipient.add(new Paragraph(resolvePartnerName(invoice.getPartnerId())).setFont(bold).setFontSize(11));
        for (String line : resolvePartnerAddress(invoice.getPartnerId())) {
            recipient.add(new Paragraph(line).setFont(regular).setFontSize(9).setMargin(0));
        }
        section.addCell(recipient);

        Cell status = noBorderCell().setTextAlignment(TextAlignment.RIGHT);
        status.add(new Paragraph("Status: " + safe(invoice.getStatus())).setFont(bold).setFontSize(9));
        status.add(new Paragraph("Currency: " + blankDefault(invoice.getCurrency(), "ZAR")).setFont(regular).setFontSize(9));
        section.addCell(status);
        document.add(section);
        document.add(new Paragraph("").setMarginBottom(1));
    }

    private void addFuneralIdentitySection(Document document, InvoiceEntity invoice, PdfFont bold, PdfFont regular) {
        if (!"FUNERAL_SERVICE".equalsIgnoreCase(safe(invoice.getSourceType()))) return;
        Optional<FuneralServiceInvoiceEntity> funeralLink = funeralServiceInvoiceRepository.findFirstByInvoiceId(invoice.getId());
        if (funeralLink.isEmpty()) return;
        FuneralServiceInvoiceEntity link = funeralLink.get();
        Table identities = new Table(UnitValue.createPercentArray(new float[]{18, 82})).useAllAvailableWidth();
        identities.setMarginBottom(10);
        addIdentityRow(identities, "Member:", combineIdentity(link.getMembershipHolderName(), link.getMembershipHolderIdentity()), bold, regular);
        addIdentityRow(identities, "Deceased:", combineIdentity(link.getDeceasedName(), link.getDeceasedIdentity()), bold, regular);
        document.add(identities);
    }

    private void addLineItems(Document document, InvoiceEntity invoice, PdfFont bold, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{58, 12, 15, 15})).useAllAvailableWidth();
        addHeaderCell(table, "Description", TextAlignment.LEFT, bold);
        addHeaderCell(table, "Quantity", TextAlignment.RIGHT, bold);
        addHeaderCell(table, "Unit Price", TextAlignment.RIGHT, bold);
        addHeaderCell(table, "Amount ZAR", TextAlignment.RIGHT, bold);

        List<InvoiceLineEntity> lines = invoice.getLines() == null ? Collections.emptyList() : invoice.getLines();
        for (InvoiceLineEntity line : lines) {
            boolean showAmount = !Boolean.FALSE.equals(line.getShowAmount());
            addBodyCell(table, safe(line.getDescription()), TextAlignment.LEFT, regular);
            addBodyCell(table, formatQuantity(line.getQuantity()), TextAlignment.RIGHT, regular);
            addBodyCell(table, showAmount ? formatCents(line.getUnitPriceCents()) : "", TextAlignment.RIGHT, regular);
            addBodyCell(table, showAmount ? formatCents(line.getTotalCents()) : "", TextAlignment.RIGHT, regular);
        }
        document.add(table);
    }

    private void addTotals(Document document, InvoiceEntity invoice, PdfFont bold, PdfFont regular) {
        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{57, 43})).useAllAvailableWidth().setMarginTop(8);
        wrapper.addCell(noBorderCell());
        Table totals = new Table(UnitValue.createPercentArray(new float[]{63, 37})).useAllAvailableWidth();
        addTotalRow(totals, "Subtotal", invoice.getSubtotalCents(), regular, regular, false);
        addTotalRow(totals, "TOTAL VAT", invoice.getTaxCents(), regular, regular, false);
        addTotalRow(totals, "TOTAL ZAR", invoice.getTotalCents(), bold, bold, true);
        addTotalRow(totals, "Less Amount Paid", invoice.getPaidCents(), regular, regular, false);
        if (value(invoice.getCreditedCents()) > 0) {
            addTotalRow(totals, "Less Credit Notes", invoice.getCreditedCents(), regular, regular, false);
        }
        addTotalRow(totals, "AMOUNT DUE ZAR", invoice.getBalanceCents(), bold, bold, true);
        wrapper.addCell(noBorderCell().setPadding(0).add(totals));
        document.add(wrapper);
    }

    private void addPaymentInformation(Document document, InvoiceEntity invoice, PdfFont bold, PdfFont regular) {
        document.add(new Paragraph("Due Date: " + date(invoice.getDueDate())).setFont(regular).setFontSize(9).setMarginTop(10).setMarginBottom(5));
        document.add(new Paragraph("Please use your invoice number as a reference for payment")
                .setFont(regular).setFontSize(9).setMarginBottom(7));

        String bank = setting("BANK-NAME");
        String holder = setting("ACCOUNT-HOLDER");
        String type = setting("ACCOUNT-TYPE");
        String number = setting("ACCOUNT-NUMBER");
        String branch = setting("BRANCH-CODE");
        if (!(bank + holder + type + number + branch).isBlank()) {
            Table banking = new Table(UnitValue.createPercentArray(new float[]{23, 77})).setWidth(UnitValue.createPercentValue(60));
            addBankRow(banking, "Bank", bank, bold, regular);
            addBankRow(banking, "Account holder", holder, bold, regular);
            addBankRow(banking, "Account Type", type, bold, regular);
            addBankRow(banking, "Account Number", number, bold, regular);
            addBankRow(banking, "Branch code", branch, bold, regular);
            document.add(banking);
        }
        if (!safe(invoice.getNotes()).isBlank() && !"FUNERAL_SERVICE".equalsIgnoreCase(safe(invoice.getSourceType()))) {
            document.add(new Paragraph(invoice.getNotes()).setFont(regular).setFontSize(8).setFontColor(TEXT_GREY).setMarginTop(8));
        }
    }

    private void addFooter(Document document, PdfFont regular) {
        String registration = firstNonBlank(
                settingService.getSetting("REGISTRATION-NUMBER", "TENANT"),
                settingService.getSetting("COMPANY-REGISTRATION-NUMBER", "TENANT"),
                settingService.getSetting("COMPANY-REG-NO", "TENANT"));
        if (!registration.isBlank()) {
            document.add(new Paragraph("Company Registration No: " + registration)
                    .setFont(regular).setFontSize(8).setFontColor(TEXT_GREY).setMarginTop(16));
        }
    }

    private void addHeaderCell(Table table, String text, TextAlignment alignment, PdfFont font) {
        table.addHeaderCell(new Cell().add(new Paragraph(text).setFont(font).setFontSize(9))
                .setBackgroundColor(HEADER_GREY).setTextAlignment(alignment).setPadding(7)
                .setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(ColorConstants.GRAY, .7f)));
    }

    private void addBodyCell(Table table, String text, TextAlignment alignment, PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(text).setFont(font).setFontSize(9))
                .setTextAlignment(alignment).setPadding(6).setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(220, 223, 228), .45f)));
    }

    private void addTotalRow(Table table, String label, Long cents, PdfFont labelFont, PdfFont valueFont, boolean strong) {
        Border border = strong ? new SolidBorder(ColorConstants.BLACK, .8f) : Border.NO_BORDER;
        table.addCell(new Cell().add(new Paragraph(label).setFont(labelFont).setFontSize(9))
                .setPadding(5).setBorder(Border.NO_BORDER).setBorderBottom(border));
        table.addCell(new Cell().add(new Paragraph(formatCents(cents)).setFont(valueFont).setFontSize(9))
                .setTextAlignment(TextAlignment.RIGHT).setPadding(5).setBorder(Border.NO_BORDER).setBorderBottom(border));
    }

    private void addIdentityRow(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        table.addCell(noBorderCell().setPadding(2).add(new Paragraph(label).setFont(bold).setFontSize(9)));
        table.addCell(noBorderCell().setPadding(2).add(new Paragraph(value).setFont(regular).setFontSize(9)));
    }

    private void addBankRow(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        if (safe(value).isBlank()) return;
        table.addCell(noBorderCell().setPadding(1).add(new Paragraph(label + ":").setFont(bold).setFontSize(9)));
        table.addCell(noBorderCell().setPadding(1).add(new Paragraph(value).setFont(regular).setFontSize(9)));
    }

    private Paragraph labelValue(String label, String value, PdfFont bold, PdfFont regular) {
        return new Paragraph().setMargin(0).setFontSize(9)
                .add(new com.itextpdf.layout.element.Text(label + "\n").setFont(regular))
                .add(new com.itextpdf.layout.element.Text(value).setFont(bold));
    }

    private Cell noBorderCell() {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0);
    }

    private String resolvePartnerName(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) return "";
        return partnerRepository.findById(partnerId).map(this::formatPartnerName).orElse(partnerId);
    }

    private String formatPartnerName(PartnerEntity partner) {
        return String.join(" ", safe(partner.getName2()).trim(), safe(partner.getName3()).trim(), safe(partner.getName1()).trim())
                .replaceAll("\\s+", " ").trim();
    }

    private List<String> resolvePartnerAddress(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) return List.of();
        List<AddressEntity> addresses = addressRepository.getByObjectId(partnerId);
        if (addresses.isEmpty()) return List.of();
        AddressEntity address = addresses.get(0);
        List<String> lines = new ArrayList<>();
        add(lines, address.getAddressLine1()); add(lines, address.getAddressLine2()); add(lines, address.getAddressLine3());
        add(lines, address.getAddressLine4()); add(lines, address.getSuburb()); add(lines, address.getTown());
        add(lines, address.getCity()); add(lines, address.getProvince()); add(lines, address.getPostalCode());
        return lines;
    }

    private void add(List<String> lines, String value) {
        if (value != null && !value.isBlank() && lines.stream().noneMatch(value::equalsIgnoreCase)) lines.add(value.trim());
    }

    private void addRightLine(Cell cell, String value, PdfFont regular) {
        if (value != null && !value.isBlank()) cell.add(new Paragraph(value).setFont(regular).setFontSize(9).setMargin(0));
    }

    private String setting(String attribute) {
        return safe(settingService.getSetting(attribute, "CASH-BANK-ACCOUNT"));
    }

    private String combineIdentity(String name, String identity) {
        String cleanName = safe(name).trim();
        String cleanIdentity = safe(identity).trim();
        return cleanIdentity.isBlank() ? cleanName : cleanName + " (" + cleanIdentity + ")";
    }

    private String prefixed(String prefix, String value) {
        return value == null || value.isBlank() ? "" : prefix + value;
    }

    private String date(java.time.LocalDate value) {
        return value == null ? "" : value.format(DATE_FORMAT);
    }

    private String formatCents(Long cents) {
        return String.format(Locale.US, "%,.2f", value(cents) / 100.0);
    }

    private String formatQuantity(Double quantity) {
        return String.format(Locale.US, "%.2f", quantity == null ? 0.0 : quantity);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private long value(Long amount) { return amount == null ? 0L : amount; }
    private String safe(String value) { return value == null ? "" : value; }
    private String blankDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
}
