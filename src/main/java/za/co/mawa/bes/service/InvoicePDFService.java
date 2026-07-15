package za.co.mawa.bes.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.v2.company.CompanyLogoEntity;
import za.co.mawa.bes.repository.PartnerRepository;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class InvoicePDFService {

    private final CompanyInfoService companyInfoService;
    private final CompanyLogoService companyLogoService;
    private final PartnerRepository partnerRepository;

    public InvoicePDFService(
            CompanyInfoService companyInfoService,
            CompanyLogoService companyLogoService,
            PartnerRepository partnerRepository) {
        this.companyInfoService = companyInfoService;
        this.companyLogoService = companyLogoService;
        this.partnerRepository = partnerRepository;
    }

    public ByteArrayOutputStream generateInvoicePdf(InvoiceEntity invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(out);

        try (Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter))) {
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            addHeader(document, boldFont, regularFont);
            document.add(new Paragraph("TAX INVOICE")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(14));

            addInvoicePartiesAndDetails(document, invoice, boldFont, regularFont);
            addLineItems(document, invoice, boldFont, regularFont);
            addTotals(document, invoice, boldFont, regularFont);

            if (invoice.getDueDate() != null) {
                document.add(new Paragraph("Due Date: " + invoice.getDueDate())
                        .setFont(regularFont)
                        .setFontSize(9)
                        .setMarginTop(10));
            }
            if (!safe(invoice.getNotes()).isBlank()) {
                document.add(new Paragraph(invoice.getNotes())
                        .setFont(regularFont)
                        .setFontSize(9)
                        .setMarginTop(4));
            }
            document.add(new Paragraph("Please use your invoice number as a reference for payment.")
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setMarginTop(8));
        } catch (Exception e) {
            throw new RuntimeException("Error while creating PDF: " + e.getMessage(), e);
        }

        return out;
    }

    private void addInvoicePartiesAndDetails(Document document, InvoiceEntity invoice, PdfFont boldFont, PdfFont regularFont) {
        Table details = new Table(UnitValue.createPercentArray(new float[]{55, 45}));
        details.setWidth(UnitValue.createPercentValue(100));

        Cell billTo = borderlessCell();
        billTo.add(new Paragraph("Bill To").setFont(boldFont).setFontSize(10));
        billTo.add(new Paragraph(resolvePartnerName(invoice.getPartnerId())).setFont(regularFont).setFontSize(10));
        details.addCell(billTo);

        Table metadata = new Table(UnitValue.createPercentArray(new float[]{45, 55}));
        metadata.setWidth(UnitValue.createPercentValue(100));
        addMetadataRow(metadata, "Invoice Date", invoice.getInvoiceDate() == null ? "" : invoice.getInvoiceDate().toString(), boldFont, regularFont);
        addMetadataRow(metadata, "Invoice Number", safe(invoice.getInvoiceNo()), boldFont, regularFont);
        addMetadataRow(metadata, "Reference", safe(invoice.getExternalRef()), boldFont, regularFont);
        addMetadataRow(metadata, "Status", safe(invoice.getStatus()), boldFont, regularFont);
        details.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(metadata));

        document.add(details);
        document.add(new Paragraph("\n"));
    }

    private void addLineItems(Document document, InvoiceEntity invoice, PdfFont boldFont, PdfFont regularFont) {
        Table itemTable = new Table(UnitValue.createPercentArray(new float[]{58, 12, 15, 15}));
        itemTable.setWidth(UnitValue.createPercentValue(100));
        itemTable.addHeaderCell(createCell("Description", boldFont, TextAlignment.LEFT));
        itemTable.addHeaderCell(createCell("Quantity", boldFont, TextAlignment.RIGHT));
        itemTable.addHeaderCell(createCell("Unit Price", boldFont, TextAlignment.RIGHT));
        itemTable.addHeaderCell(createCell("Amount ZAR", boldFont, TextAlignment.RIGHT));

        List<InvoiceLineEntity> lines = invoice.getLines() == null ? Collections.emptyList() : invoice.getLines();
        for (InvoiceLineEntity line : lines) {
            itemTable.addCell(createCell(safe(line.getDescription()), regularFont, TextAlignment.LEFT));
            itemTable.addCell(createCell(formatQuantity(line.getQuantity()), regularFont, TextAlignment.RIGHT));
            itemTable.addCell(createCell(formatCents(line.getUnitPriceCents()), regularFont, TextAlignment.RIGHT));
            itemTable.addCell(createCell(formatCents(line.getTotalCents()), regularFont, TextAlignment.RIGHT));
        }
        document.add(itemTable);
    }

    private void addTotals(Document document, InvoiceEntity invoice, PdfFont boldFont, PdfFont regularFont) {
        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{55, 45}));
        wrapper.setWidth(UnitValue.createPercentValue(100));
        wrapper.setMarginTop(8);
        wrapper.addCell(borderlessCell());

        Table totals = new Table(UnitValue.createPercentArray(new float[]{62, 38}));
        totals.setWidth(UnitValue.createPercentValue(100));
        addTotalRow(totals, "Subtotal", invoice.getSubtotalCents(), regularFont, regularFont);
        addTotalRow(totals, "TOTAL VAT", invoice.getTaxCents(), regularFont, regularFont);
        addTotalRow(totals, "TOTAL ZAR", invoice.getTotalCents(), boldFont, boldFont);
        addTotalRow(totals, "Less Amount Paid", invoice.getPaidCents(), regularFont, regularFont);
        addTotalRow(totals, "AMOUNT DUE ZAR", invoice.getBalanceCents(), boldFont, boldFont);
        wrapper.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(totals));
        document.add(wrapper);
    }

    private void addHeader(Document document, PdfFont boldFont, PdfFont regularFont) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        header.setWidth(UnitValue.createPercentValue(100));
        Cell logoCell = borderlessCell();
        Optional<CompanyLogoEntity> logoEntity = companyLogoService.getActiveLogo();
        if (logoEntity.isPresent()) {
            ImageData logoData = ImageDataFactory.create(logoEntity.get().getContent());
            Image logo = new Image(logoData).scaleToFit(CompanyLogoService.PDF_WIDTH_PT, CompanyLogoService.PDF_HEIGHT_PT);
            logoCell.add(logo);
        } else {
            logoCell.add(new Paragraph("COMPANY\nLOGO")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFont(boldFont)
                    .setFontSize(9)
                    .setBorder(new com.itextpdf.layout.borders.SolidBorder(1))
                    .setWidth(CompanyLogoService.PDF_WIDTH_PT)
                    .setHeight(CompanyLogoService.PDF_HEIGHT_PT));
        }
        header.addCell(logoCell);

        String companyName = blankDefault(companyInfoService.getCompanyName(), "Company Name");
        String address = blankDefault(companyInfoService.getCompanyAddress(), "");
        String tel = blankDefault(companyInfoService.getCompanyTelephoneNumber(), "");
        String vat = blankDefault(companyInfoService.getVATNumber(), "");
        Cell infoCell = borderlessCell();
        infoCell.add(new Paragraph(companyName).setFont(boldFont).setFontSize(14).setTextAlignment(TextAlignment.RIGHT));
        if (!address.isBlank()) infoCell.add(new Paragraph(address).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
        if (!tel.isBlank()) infoCell.add(new Paragraph("Tel: " + tel).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
        if (!vat.isBlank()) infoCell.add(new Paragraph("VAT No: " + vat).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
        header.addCell(infoCell);
        document.add(header);
        document.add(new Paragraph("\n"));
    }

    private String resolvePartnerName(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) return "";
        return partnerRepository.findById(partnerId)
                .map(this::formatPartnerName)
                .orElse(partnerId);
    }

    private String formatPartnerName(PartnerEntity partner) {
        String name = String.join(" ",
                safe(partner.getName1()).trim(),
                safe(partner.getName2()).trim(),
                safe(partner.getName3()).trim()).trim().replaceAll("\\s+", " ");
        return name.isBlank() ? safe(partner.getNo()) : name;
    }

    private void addMetadataRow(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont) {
        table.addCell(createBorderlessTextCell(label, boldFont, TextAlignment.LEFT));
        table.addCell(createBorderlessTextCell(value, regularFont, TextAlignment.LEFT));
    }

    private void addTotalRow(Table table, String label, Long cents, PdfFont labelFont, PdfFont valueFont) {
        table.addCell(createBorderlessTextCell(label, labelFont, TextAlignment.RIGHT));
        table.addCell(createBorderlessTextCell(formatCents(cents), valueFont, TextAlignment.RIGHT));
    }

    private Cell borderlessCell() {
        return new Cell().setBorder(Border.NO_BORDER).setPadding(0);
    }

    private Cell createBorderlessTextCell(String content, PdfFont font, TextAlignment alignment) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(3)
                .setTextAlignment(alignment)
                .add(new Paragraph(content == null ? "" : content).setFont(font).setFontSize(9));
    }

    private Cell createCell(String content, PdfFont font, TextAlignment alignment) {
        return new Cell()
                .setPadding(5)
                .setTextAlignment(alignment)
                .add(new Paragraph(content == null ? "" : content).setFont(font).setFontSize(9));
    }

    private String formatCents(Long cents) {
        return String.format(Locale.US, "%,.2f", (cents == null ? 0L : cents) / 100.0);
    }

    private String formatQuantity(Double quantity) {
        double value = quantity == null ? 0.0 : quantity;
        return String.format(Locale.US, "%.2f", value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String blankDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
