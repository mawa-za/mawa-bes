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

import java.io.ByteArrayOutputStream;

@Service
public class InvoicePDFService {

    public ByteArrayOutputStream generateInvoicePdf(InvoiceEntity invoice) {
        // Create a PDF in memory
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(out);

        try (Document document = new Document(new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter))) {
            // Load custom fonts (optional)
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // --- HEADER (Company Logo and Info) ---
            ImageData logoData = ImageDataFactory.create(getClass().getResource("images/logo.png"));
//            ImageData logoData = ImageDataFactory.create("https://upload.wikimedia.org/wikipedia/commons/3/3f/Logo_Test.png");  // Replace with the actual path to your logo
            Image logo = new Image(logoData).scaleToFit(150, 40); // Resize the logo for better fit
            document.add(new Paragraph().add(logo).setTextAlignment(TextAlignment.LEFT));

            document.add(new Paragraph("Your Company Name")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.LEFT));
            document.add(new Paragraph("123 Business Street, City, Country")
                    .setFont(regularFont)
                    .setFontSize(10));
            document.add(new Paragraph("Phone: +123-456-7890 | Email: contact@yourcompany.com")
                    .setFont(regularFont)
                    .setFontSize(10));

            // Add spacing
            document.add(new Paragraph("\n"));

            // --- TITLE: Invoice ---
            document.add(new Paragraph("Invoice")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // --- INVOICE DETAILS TABLE ---
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
            detailsTable.setWidth(UnitValue.createPercentValue(100));
            detailsTable.addCell(createCell("Invoice ID:", boldFont));
            detailsTable.addCell(createCell(invoice.getId(), regularFont));
            detailsTable.addCell(createCell("Invoice Date:", boldFont));
            detailsTable.addCell(createCell(invoice.getInvoiceDate().toString(), regularFont));
            detailsTable.addCell(createCell("Due Date:", boldFont));
            detailsTable.addCell(createCell(invoice.getDueDate().toString(), regularFont));
            detailsTable.addCell(createCell("Status:", boldFont));
            detailsTable.addCell(createCell(invoice.getStatus(), regularFont));
            document.add(detailsTable);

            // Add spacing
            document.add(new Paragraph("\n"));

            // --- LINE ITEMS TABLE ---
            document.add(new Paragraph("Line Items")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginBottom(10));

            // Line Items Table Structure
            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{3, 7, 2, 3, 3, 3}));
            itemTable.setWidth(UnitValue.createPercentValue(100));

            // Header
            itemTable.addHeaderCell(createCell("Product ID", boldFont));
            itemTable.addHeaderCell(createCell("Description", boldFont));
            itemTable.addHeaderCell(createCell("Qty", boldFont));
            itemTable.addHeaderCell(createCell("Unit Price", boldFont));
            itemTable.addHeaderCell(createCell("Subtotal", boldFont));
            itemTable.addHeaderCell(createCell("Tax", boldFont));

            // Line-items rows
            for (InvoiceLineEntity line : invoice.getLines()) {
                itemTable.addCell(createCell(line.getProductId(), regularFont));
                itemTable.addCell(createCell(line.getDescription(), regularFont));
                itemTable.addCell(createCell(String.valueOf(line.getQuantity()), regularFont));
                itemTable.addCell(createCell(String.format("%.2f", line.getUnitPriceCents() / 100.0), regularFont));
                itemTable.addCell(createCell(String.format("%.2f", line.getSubtotalCents() / 100.0), regularFont));
                itemTable.addCell(createCell(String.format("%.2f", line.getTaxCents() / 100.0), regularFont));
            }

            document.add(itemTable);

            // --- TOTAL AMOUNT ---
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Total: $" + String.format("%.2f", invoice.getTotalCents() / 100.0))
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.RIGHT));

            // --- FOOTER NOTE ---
            document.add(new Paragraph("\nThank you for your business!")
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while creating PDF: " + e.getMessage());
        }

        return out;
    }

    /**
     * Helper method to create formatted table cells.
     *
     * @param content The text content of the cell.
     * @param font    The font to use for the text.
     * @return A formatted `Cell` object.
     */
    private Cell createCell(String content, PdfFont font) {
        return new Cell().add(new Paragraph(content).setFont(font).setFontSize(10))
                .setPadding(5)
                .setTextAlignment(TextAlignment.LEFT);
    }
}