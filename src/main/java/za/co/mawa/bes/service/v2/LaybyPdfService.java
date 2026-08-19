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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.service.CompanyPdfBrandingService;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LaybyPdfService {

    private final LaybyManagementService laybyService;
    private final CompanyPdfBrandingService companyPdfBrandingService;

    public byte[] agreement(String id) {
        Map<String, Object> layby = laybyService.get(id);
        return render(layby, false);
    }

    public byte[] statement(String id) {
        Map<String, Object> layby = laybyService.get(id);
        return render(layby, true);
    }

    @SuppressWarnings("unchecked")
    private byte[] render(Map<String, Object> layby, boolean statement) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            companyPdfBrandingService.addITextHeader(doc, regular, bold);
            doc.add(new Paragraph(statement ? "LAYBY STATEMENT" : "LAYBY AGREEMENT").setFont(bold).setFontSize(18).setMarginTop(12));
            Table header = new Table(new float[]{1, 2});
            header.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
            row(header, "Layby number", text(layby.get("layby_no")), bold, regular);
            row(header, "Customer", text(layby.get("customer_name")), bold, regular);
            row(header, "Customer number", text(layby.get("customer_no")), bold, regular);
            row(header, "Status", text(layby.get("status")), bold, regular);
            row(header, "Sales order", text(layby.get("sales_order_no")), bold, regular);
            row(header, "Agreement total", money(layby.get("total_cents")), bold, regular);
            row(header, "Paid", money(layby.get("paid_cents")), bold, regular);
            row(header, "Outstanding", money(layby.get("balance_cents")), bold, regular);
            row(header, "Payment frequency", text(layby.get("payment_frequency")), bold, regular);
            row(header, "Required deposit", money(layby.get("deposit_required_cents")), bold, regular);
            row(header, "Expected completion", text(layby.get("expected_completion_date")), bold, regular);
            row(header, "Cancellation penalty", text(layby.get("cancellation_penalty_percent")) + "%", bold, regular);
            row(header, "Default grace period", text(layby.get("grace_business_days")) + " business days", bold, regular);
            doc.add(header);

            List<Map<String, Object>> installments = (List<Map<String, Object>>) layby.getOrDefault("installments", List.of());
            doc.add(new Paragraph("Payment schedule").setFont(bold).setFontSize(12).setMarginTop(16));
            Table schedule = new Table(new float[]{1, 2, 2, 2, 2, 2});
            schedule.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
            for (String h : List.of("#", "Due date", "Amount", "Paid", "Balance", "Status")) {
                schedule.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold).setFontSize(9)));
            }
            for (Map<String, Object> i : installments) {
                schedule.addCell(cell(text(i.get("installment_no")), regular));
                schedule.addCell(cell(text(i.get("due_date")), regular));
                schedule.addCell(cell(money(i.get("amount_cents")), regular));
                schedule.addCell(cell(money(i.get("paid_cents")), regular));
                schedule.addCell(cell(money(i.get("balance_cents")), regular));
                schedule.addCell(cell(text(i.get("status")), regular));
            }
            doc.add(schedule);

            if (statement) {
                List<Map<String, Object>> payments = (List<Map<String, Object>>) layby.getOrDefault("payments", List.of());
                doc.add(new Paragraph("Payments received").setFont(bold).setFontSize(12).setMarginTop(16));
                Table paymentTable = new Table(new float[]{2, 2, 2, 2});
                paymentTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
                for (String h : List.of("Receipt", "Date", "Method", "Amount")) {
                    paymentTable.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold).setFontSize(9)));
                }
                for (Map<String, Object> payment : payments) {
                    paymentTable.addCell(cell(text(payment.get("receipt_no")), regular));
                    paymentTable.addCell(cell(text(payment.get("receipt_date")), regular));
                    paymentTable.addCell(cell(text(payment.get("payment_method")), regular));
                    paymentTable.addCell(cell(money(payment.get("allocated_amount_cents")), regular));
                }
                doc.add(paymentTable);
            } else {
                doc.add(new Paragraph("Agreement terms").setFont(bold).setFontSize(12).setMarginTop(16));
                doc.add(new Paragraph(
                        "Goods remain reserved for the layby and are released only after the agreement is fully paid and fulfilled. " +
                        "Early and partial payments are accepted. Cancellation is subject to the configured layby cancellation rules; " +
                        "where a cancellation penalty applies it may not exceed the recorded agreement percentage. " +
                        "No cancellation penalty is applied for a recorded death or hospitalisation cancellation reason."
                ).setFont(regular).setFontSize(9));
                doc.add(new Paragraph("Terms version: " + text(layby.get("terms_version"))).setFont(regular).setFontSize(9));
                doc.add(new Paragraph("Accepted by: " + text(layby.get("terms_accepted_by")) + " at " + text(layby.get("terms_accepted_at"))).setFont(regular).setFontSize(9));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate layby PDF", exception);
        }
    }

    private void row(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        table.addCell(cell(label, bold));
        table.addCell(cell(value, regular));
    }

    private Cell cell(String value, PdfFont font) {
        return new Cell().add(new Paragraph(value == null ? "" : value).setFont(font).setFontSize(9));
    }

    private String money(Object cents) {
        BigDecimal amount = BigDecimal.valueOf(longValue(cents)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return "R " + amount.toPlainString();
    }

    private long longValue(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        return new BigDecimal(value.toString()).longValue();
    }

    private String text(Object value) {
        return value == null ? "" : Objects.toString(value, "");
    }

}
