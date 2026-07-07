package za.co.mawa.bes.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.InvoiceLineEntity;
import za.co.mawa.bes.entity.v2.company.CompanyLogoEntity;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

@Service
public class InvoicePDFService {

    private final CompanyInfoService companyInfoService;
    private final CompanyLogoService companyLogoService;

    public InvoicePDFService(CompanyInfoService companyInfoService, CompanyLogoService companyLogoService) {
        this.companyInfoService = companyInfoService;
        this.companyLogoService = companyLogoService;
    }

    public ByteArrayOutputStream generateInvoicePdf(InvoiceEntity invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(out);

        try (Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter))) {
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            addHeader(document, boldFont, regularFont);

            document.add(new Paragraph("Invoice")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
            detailsTable.setWidth(UnitValue.createPercentValue(100));
            detailsTable.addCell(createCell("Invoice No:", boldFont));
            detailsTable.addCell(createCell(safe(invoice.getInvoiceNo()), regularFont));
            detailsTable.addCell(createCell("Invoice Date:", boldFont));
            detailsTable.addCell(createCell(invoice.getInvoiceDate() == null ? "" : invoice.getInvoiceDate().toString(), regularFont));
            detailsTable.addCell(createCell("Due Date:", boldFont));
            detailsTable.addCell(createCell(invoice.getDueDate() == null ? "" : invoice.getDueDate().toString(), regularFont));
            detailsTable.addCell(createCell("Status:", boldFont));
            detailsTable.addCell(createCell(safe(invoice.getStatus()), regularFont));
            document.add(detailsTable);

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Line Items")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginBottom(10));

            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{3, 7, 2, 3, 3, 3}));
            itemTable.setWidth(UnitValue.createPercentValue(100));
            itemTable.addHeaderCell(createCell("Product ID", boldFont));
            itemTable.addHeaderCell(createCell("Description", boldFont));
            itemTable.addHeaderCell(createCell("Qty", boldFont));
            itemTable.addHeaderCell(createCell("Unit Price", boldFont));
            itemTable.addHeaderCell(createCell("Subtotal", boldFont));
            itemTable.addHeaderCell(createCell("Tax", boldFont));

            for (InvoiceLineEntity line : invoice.getLines()) {
                itemTable.addCell(createCell(safe(line.getProductId()), regularFont));
                itemTable.addCell(createCell(safe(line.getDescription()), regularFont));
                itemTable.addCell(createCell(String.valueOf(line.getQuantity()), regularFont));
                itemTable.addCell(createCell(formatCents(line.getUnitPriceCents()), regularFont));
                itemTable.addCell(createCell(formatCents(line.getSubtotalCents()), regularFont));
                itemTable.addCell(createCell(formatCents(line.getTaxCents()), regularFont));
            }

            document.add(itemTable);
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Total: R " + formatCents(invoice.getTotalCents()))
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("\nThank you for your business.")
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));

        } catch (Exception e) {
            throw new RuntimeException("Error while creating PDF: " + e.getMessage(), e);
        }

        return out;
    }

    private void addHeader(Document document, PdfFont boldFont, PdfFont regularFont) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        header.setWidth(UnitValue.createPercentValue(100));
        Cell logoCell = new Cell().setBorder(null).setPadding(0);
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
        Cell infoCell = new Cell().setBorder(null).setPadding(0);
        infoCell.add(new Paragraph(companyName).setFont(boldFont).setFontSize(14).setTextAlignment(TextAlignment.RIGHT));
        if (!address.isBlank()) infoCell.add(new Paragraph(address).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
        if (!tel.isBlank()) infoCell.add(new Paragraph("Tel: " + tel).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
        if (!vat.isBlank()) infoCell.add(new Paragraph("VAT No: " + vat).setFont(regularFont).setFontSize(10).setTextAlignment(TextAlignment.RIGHT));
        header.addCell(infoCell);
        document.add(header);
        document.add(new Paragraph("\n"));
    }

    private String formatCents(Long cents) {
        return String.format("%.2f", (cents == null ? 0L : cents) / 100.0);
    }

    private String safe(String value) { return value == null ? "" : value; }
    private String blankDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    private Cell createCell(String content, PdfFont font) {
        return new Cell().add(new Paragraph(content == null ? "" : content).setFont(font).setFontSize(10))
                .setPadding(5)
                .setTextAlignment(TextAlignment.LEFT);
    }
}
