package za.co.mawa.bes.service.v2;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.service.CompanyPdfBrandingService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class QuotationPdfService {
    private final StockOperationsService stockOperationsService;
    private final CompanyPdfBrandingService companyPdfBrandingService;

    public QuotationPdfService(StockOperationsService stockOperationsService, CompanyPdfBrandingService companyPdfBrandingService) {
        this.stockOperationsService = stockOperationsService;
        this.companyPdfBrandingService = companyPdfBrandingService;
    }

    public byte[] generate(String quotationId) {
        Map<String, Object> q = stockOperationsService.getQuotation(quotationId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            companyPdfBrandingService.addITextHeader(doc, regular, bold);

            String title = text(q.get("title"));
            doc.add(new Paragraph(title.isBlank() ? "QUOTATION" : title).setFont(bold).setFontSize(18));
            String summary = text(q.get("summary"));
            if (!summary.isBlank()) doc.add(new Paragraph(summary).setFont(regular).setFontSize(10));

            Table details = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2})).useAllAvailableWidth();
            detail(details, "Quotation No", q.get("quotation_no"), bold, regular);
            detail(details, "Customer", q.get("customer_name"), bold, regular);
            detail(details, "Quotation Date", q.get("quotation_date"), bold, regular);
            detail(details, "Valid Until", q.get("valid_until"), bold, regular);
            doc.add(details);
            doc.add(new Paragraph("Items").setFont(bold).setFontSize(12));

            Table lines = new Table(UnitValue.createPercentArray(new float[]{1.2f, 4f, 1f, 1.5f, 1.5f})).useAllAvailableWidth();
            for (String h : new String[]{"Code", "Description", "Qty", "Unit Price", "Total"}) {
                lines.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold).setFontSize(9)));
            }
            Object raw = q.get("lines");
            if (raw instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> line)) continue;
                    lines.addCell(cell(line.get("product_code"), regular));
                    lines.addCell(cell(line.get("product_description"), regular));
                    lines.addCell(cell(line.get("quantity"), regular));
                    lines.addCell(moneyCell(line.get("unit_price"), regular));
                    lines.addCell(moneyCell(line.get("line_total"), regular));
                }
            }
            doc.add(lines);

            Table totals = new Table(UnitValue.createPercentArray(new float[]{3, 1.2f})).setWidth(UnitValue.createPercentValue(45)).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT);
            total(totals, "Subtotal", q.get("subtotal_amount"), bold, regular);
            total(totals, "VAT", q.get("tax_amount"), bold, regular);
            total(totals, "Total", q.get("total_amount"), bold, bold);
            doc.add(totals);
            String notes = text(q.get("notes"));
            if (!notes.isBlank()) doc.add(new Paragraph("Notes").setFont(bold).setFontSize(10)).add(new Paragraph(notes).setFont(regular).setFontSize(9));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate quotation PDF", e);
        }
    }

    private void detail(Table t, String label, Object value, PdfFont bold, PdfFont regular) {
        t.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(9)));
        t.addCell(new Cell().add(new Paragraph(text(value)).setFont(regular).setFontSize(9)));
    }
    private Cell cell(Object value, PdfFont font) { return new Cell().add(new Paragraph(text(value)).setFont(font).setFontSize(9)); }
    private Cell moneyCell(Object value, PdfFont font) { return new Cell().setTextAlignment(TextAlignment.RIGHT).add(new Paragraph("R " + money(value)).setFont(font).setFontSize(9)); }
    private void total(Table t, String label, Object value, PdfFont labelFont, PdfFont valueFont) {
        t.addCell(new Cell().setTextAlignment(TextAlignment.RIGHT).add(new Paragraph(label).setFont(labelFont).setFontSize(9)));
        t.addCell(new Cell().setTextAlignment(TextAlignment.RIGHT).add(new Paragraph("R " + money(value)).setFont(valueFont).setFontSize(9)));
    }
    private String text(Object value) { return value == null ? "" : value.toString(); }
    private String money(Object value) { try { return new BigDecimal(text(value)).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); } catch (Exception e) { return "0.00"; } }
}
