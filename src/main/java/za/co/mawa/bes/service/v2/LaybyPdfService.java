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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LaybyPdfService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

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
    public byte[] cancellationForm(String id) {
        Map<String, Object> layby = laybyService.get(id);
        Map<String, Object> refund = layby.get("refund") instanceof Map
                ? (Map<String, Object>) layby.get("refund") : Map.of();
        if (refund.isEmpty()) {
            throw new IllegalStateException("A cancellation must be requested before the cancellation form can be generated");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            companyPdfBrandingService.addITextHeader(doc, regular, bold);
            doc.add(new Paragraph("LAYBY CANCELLATION & CUSTOMER REFUND FORM")
                    .setFont(bold).setFontSize(16).setMarginTop(12));
            doc.add(new Paragraph(
                    "This form records the customer's or authorised representative's instruction to cancel the layby. "
                            + "The signed form must be uploaded to the Layby. Where automatic customer refund Payment Requests are enabled, "
                            + "MawaERP also attaches the signed form to that Payment Request before approval.")
                    .setFont(regular).setFontSize(9));

            Table details = new Table(new float[]{1, 2});
            details.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
            row(details, "Layby number", text(layby.get("layby_no")), bold, regular);
            row(details, "Customer", text(layby.get("customer_name")), bold, regular);
            row(details, "Customer number", text(layby.get("customer_no")), bold, regular);
            row(details, "Sales order", text(layby.get("sales_order_no")), bold, regular);
            row(details, "Cancellation requested", dateTime(layby.get("cancellation_requested_at")), bold, regular);
            row(details, "Reason category", text(layby.get("cancellation_reason_code")), bold, regular);
            row(details, "Cancellation reason", text(layby.get("cancellation_reason")), bold, regular);
            row(details, "Gross paid", money(refund.get("gross_paid_cents")), bold, regular);
            row(details, "Cancellation penalty", money(refund.get("penalty_cents")), bold, regular);
            row(details, "Refund amount", money(refund.get("refund_amount_cents")), bold, regular);
            row(details, "Refund method", text(refund.get("refund_method")), bold, regular);
            String paymentRequestNo = text(refund.get("payment_request_no"));
            row(details, "Payment request", paymentRequestNo.isBlank() ? "Not generated" : paymentRequestNo, bold, regular);
            doc.add(details);

            doc.add(new Paragraph("Customer / Representative declaration")
                    .setFont(bold).setFontSize(12).setMarginTop(20));
            doc.add(new Paragraph(
                    "I confirm that the cancellation details and refund amount shown above are correct and request that the layby be cancelled. "
                            + "Where I sign as a representative, I confirm that I am authorised to act for the customer.")
                    .setFont(regular).setFontSize(9));

            Table signatures = new Table(new float[]{1, 2});
            signatures.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
            row(signatures, "Signed by (print name)", "____________________________________________", bold, regular);
            row(signatures, "Capacity", "Customer / Representative (circle one)", bold, regular);
            row(signatures, "Signature", "____________________________________________", bold, regular);
            row(signatures, "Date", "____________________________________________", bold, regular);
            row(signatures, "Contact number", "____________________________________________", bold, regular);
            doc.add(signatures);

            doc.add(new Paragraph("For office use")
                    .setFont(bold).setFontSize(12).setMarginTop(20));
            doc.add(new Paragraph(
                    "After signature, scan or photograph this completed form and upload it from the Layby screen. "
                            + "MawaERP keeps the signed form on the Layby cancellation. If automatic customer refund Payment Requests are enabled, "
                            + "the same signed form is also attached to the Payment Request before it is submitted for approval.")
                    .setFont(regular).setFontSize(9));

            doc.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate layby cancellation form", exception);
        }
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
            row(header, "Expected completion", date(layby.get("expected_completion_date")), bold, regular);
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
                schedule.addCell(cell(date(i.get("due_date")), regular));
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
                    paymentTable.addCell(cell(dateTime(payment.get("receipt_date")), regular));
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
                doc.add(new Paragraph("Accepted by: " + text(layby.get("terms_accepted_by")) + " at " + dateTime(layby.get("terms_accepted_at"))).setFont(regular).setFontSize(9));
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

    private String date(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDate localDate) return DISPLAY_DATE.format(localDate);
        if (value instanceof java.sql.Date sqlDate) return DISPLAY_DATE.format(sqlDate.toLocalDate());
        if (value instanceof LocalDateTime localDateTime) return DISPLAY_DATE.format(localDateTime.toLocalDate());
        if (value instanceof Timestamp timestamp) return DISPLAY_DATE.format(timestamp.toLocalDateTime().toLocalDate());
        String raw = text(value).trim();
        if (raw.isEmpty()) return "";
        try { return DISPLAY_DATE.format(LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw)); }
        catch (Exception ignored) { return raw; }
    }

    private String dateTime(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDateTime localDateTime) return DISPLAY_DATE_TIME.format(localDateTime);
        if (value instanceof Timestamp timestamp) return DISPLAY_DATE_TIME.format(timestamp.toLocalDateTime());
        if (value instanceof LocalDate localDate) return DISPLAY_DATE.format(localDate);
        if (value instanceof java.sql.Date sqlDate) return DISPLAY_DATE.format(sqlDate.toLocalDate());
        String raw = text(value).trim();
        if (raw.isEmpty()) return "";
        try { return DISPLAY_DATE_TIME.format(LocalDateTime.parse(raw.replace(' ', 'T'))); }
        catch (Exception ignored) {
            try { return DISPLAY_DATE.format(LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw)); }
            catch (Exception ignoredAgain) { return raw; }
        }
    }

    private String text(Object value) {
        return value == null ? "" : Objects.toString(value, "");
    }

}
