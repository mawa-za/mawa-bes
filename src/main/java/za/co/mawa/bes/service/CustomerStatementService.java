package za.co.mawa.bes.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.repository.PartnerRepository;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerStatementService {
    private final JdbcTemplate jdbcTemplate;
    private final PartnerRepository partnerRepository;
    private final CompanyPdfBrandingService companyPdfBrandingService;

    public Map<String, Object> generate(String partnerId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) fromDate = LocalDate.now().minusMonths(3).withDayOfMonth(1);
        if (toDate == null) toDate = LocalDate.now();
        if (toDate.isBefore(fromDate)) throw new IllegalArgumentException("Statement end date cannot be before start date");
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + partnerId));

        long opening = queryLong("SELECT COALESCE(SUM(total_cents),0) FROM invoice WHERE partner_id=? AND invoice_date < ?", partnerId, fromDate)
                - queryLong("SELECT COALESCE(SUM(ip.amount_cents),0) FROM invoice_payment ip JOIN invoice i ON i.id=ip.invoice_id WHERE i.partner_id=? AND DATE(ip.payment_date) < ?", partnerId, fromDate)
                - queryLong("SELECT COALESCE(SUM(total_cents),0) FROM credit_note WHERE partner_id=? AND credit_note_date < ? AND status='ISSUED'", partnerId, fromDate);

        List<StatementEntry> entries = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT invoice_date, invoice_no, external_ref, total_cents
                  FROM invoice WHERE partner_id=? AND invoice_date BETWEEN ? AND ?
                """, rs -> {
                    entries.add(new StatementEntry(rs.getDate(1).toLocalDate(), "INVOICE", rs.getString(2), rs.getString(3), rs.getLong(4), 0L));
                }, partnerId, fromDate, toDate);
        jdbcTemplate.query("""
                SELECT DATE(ip.payment_date), COALESCE(ip.reference_no,'Payment'), ip.payment_method, ip.amount_cents
                  FROM invoice_payment ip JOIN invoice i ON i.id=ip.invoice_id
                 WHERE i.partner_id=? AND DATE(ip.payment_date) BETWEEN ? AND ?
                """, rs -> {
                    entries.add(new StatementEntry(rs.getDate(1).toLocalDate(), "PAYMENT", rs.getString(2), rs.getString(3), 0L, rs.getLong(4)));
                }, partnerId, fromDate, toDate);
        jdbcTemplate.query("""
                SELECT credit_note_date, credit_note_no, reason, total_cents
                  FROM credit_note WHERE partner_id=? AND credit_note_date BETWEEN ? AND ? AND status='ISSUED'
                """, rs -> {
                    entries.add(new StatementEntry(rs.getDate(1).toLocalDate(), "CREDIT_NOTE", rs.getString(2), rs.getString(3), 0L, rs.getLong(4)));
                }, partnerId, fromDate, toDate);
        entries.sort(Comparator.comparing(StatementEntry::date).thenComparing(StatementEntry::type).thenComparing(StatementEntry::reference));

        long running = opening;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StatementEntry entry : entries) {
            running += entry.debitCents - entry.creditCents;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", entry.date); row.put("type", entry.type); row.put("reference", entry.reference);
            row.put("description", entry.description == null ? "" : entry.description);
            row.put("debitCents", entry.debitCents); row.put("creditCents", entry.creditCents); row.put("balanceCents", running);
            rows.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("partnerId", partnerId); result.put("partnerNo", partner.getNo()); result.put("partnerName", partnerName(partner));
        result.put("fromDate", fromDate); result.put("toDate", toDate); result.put("openingBalanceCents", opening);
        result.put("entries", rows); result.put("closingBalanceCents", running);
        return result;
    }

    public byte[] generatePdf(String partnerId, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> statement = generate(partnerId, fromDate, toDate);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document document = new Document(pdf)) {
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            companyPdfBrandingService.addITextHeader(document, regular, bold);
            document.add(new Paragraph("CUSTOMER STATEMENT").setFont(bold).setFontSize(18)
                    .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(8));
            document.add(new Paragraph(statement.get("partnerName") + "  " + defaultString(statement.get("partnerNo"), ""))
                    .setFont(bold).setFontSize(11));
            document.add(new Paragraph("Period: " + statement.get("fromDate") + " to " + statement.get("toDate"))
                    .setFont(regular).setFontSize(9).setMarginBottom(12));
            document.add(new Paragraph("Opening balance: R " + formatCents(statement.get("openingBalanceCents")))
                    .setFont(bold).setFontSize(9));
            Table table = new Table(UnitValue.createPercentArray(new float[]{13, 16, 18, 25, 14, 14})).useAllAvailableWidth();
            for (String heading : List.of("Date", "Type", "Reference", "Description", "Debit", "Credit / Balance"))
                table.addHeaderCell(cell(heading, bold, 8, TextAlignment.LEFT));
            @SuppressWarnings("unchecked") List<Map<String,Object>> entries = (List<Map<String,Object>>) statement.get("entries");
            for (Map<String,Object> entry : entries) {
                table.addCell(cell(entry.get("date").toString(), regular, 8, TextAlignment.LEFT));
                table.addCell(cell(entry.get("type").toString(), regular, 8, TextAlignment.LEFT));
                table.addCell(cell(defaultString(entry.get("reference"), ""), regular, 8, TextAlignment.LEFT));
                table.addCell(cell(defaultString(entry.get("description"), ""), regular, 8, TextAlignment.LEFT));
                table.addCell(cell(formatCents(entry.get("debitCents")), regular, 8, TextAlignment.RIGHT));
                table.addCell(cell(formatCents(entry.get("creditCents")) + " / " + formatCents(entry.get("balanceCents")), regular, 8, TextAlignment.RIGHT));
            }
            document.add(table);
            document.add(new Paragraph("Closing balance ZAR: " + formatCents(statement.get("closingBalanceCents")))
                    .setFont(bold).setFontSize(11).setTextAlignment(TextAlignment.RIGHT).setMarginTop(12));
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate customer statement PDF", exception);
        }
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
    private String partnerName(PartnerEntity partner) {
        return (defaultString(partner.getName2(), "") + " " + defaultString(partner.getName3(), "") + " " + defaultString(partner.getName1(), "")).trim().replaceAll("\\s+", " ");
    }
    private Cell cell(String text, PdfFont font, int size, TextAlignment alignment) {
        return new Cell().setBorder(Border.NO_BORDER).setTextAlignment(alignment).setPadding(4)
                .add(new Paragraph(defaultString(text, "")).setFont(font).setFontSize(size));
    }
    private String defaultString(Object value, String fallback) { return value == null || value.toString().isBlank() ? fallback : value.toString(); }
    private String formatCents(Object value) {
        long cents = value instanceof Number number ? number.longValue() : Long.parseLong(defaultString(value, "0"));
        return String.format(Locale.US, "%,.2f", cents / 100.0);
    }
    private record StatementEntry(LocalDate date, String type, String reference, String description, long debitCents, long creditCents) {}
}
