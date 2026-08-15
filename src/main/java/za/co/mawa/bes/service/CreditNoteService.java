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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.dto.creditnote.CreditNoteIssueRequestDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.service.v2.NumberAllocationService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditNoteService {
    private final InvoiceRepository invoiceRepository;
    private final JdbcTemplate jdbcTemplate;
    private final NumberAllocationService numberAllocationService;
    private final CompanyPdfBrandingService companyPdfBrandingService;

    @Transactional
    public Map<String, Object> issue(String invoiceId, CreditNoteIssueRequestDto request, String userId) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new IllegalArgumentException("A credit note reason is required");
        }
        long available = Math.max(0L, value(invoice.getTotalCents()) - value(invoice.getCreditedCents()));
        long amount = request.getAmountCents() == null ? available : request.getAmountCents();
        if (amount <= 0) throw new IllegalArgumentException("Credit note amount must be greater than zero");
        if (amount > available) throw new IllegalArgumentException("Credit note amount exceeds the remaining creditable invoice amount");

        String id = UUID.randomUUID().toString();
        String number = generateNumber();
        LocalDate date = LocalDate.now();
        jdbcTemplate.update("""
                INSERT INTO credit_note
                (id, credit_note_no, invoice_id, partner_id, credit_note_date, reason,
                 subtotal_cents, tax_cents, total_cents, currency, status, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 'ISSUED', ?, CURRENT_TIMESTAMP)
                """, id, number, invoiceId, invoice.getPartnerId(), date, request.getReason().trim(), amount, amount,
                defaultString(invoice.getCurrency(), "ZAR"), userId);
        jdbcTemplate.update("""
                INSERT INTO credit_note_line
                (id, credit_note_id, description, quantity, unit_price_cents, tax_cents, subtotal_cents, total_cents, created_at)
                VALUES (?, ?, ?, 1, ?, 0, ?, ?, CURRENT_TIMESTAMP)
                """, UUID.randomUUID().toString(), id,
                "Credit against " + defaultString(invoice.getInvoiceNo(), invoiceId), amount, amount, amount);

        long credited = value(invoice.getCreditedCents()) + amount;
        invoice.setCreditedCents(credited);
        invoice.setBalanceCents(Math.max(0L, value(invoice.getTotalCents()) - value(invoice.getPaidCents()) - credited));
        invoice.setStatus(invoice.getBalanceCents() == 0
                ? "CREDITED"
                : (credited > 0 ? "PARTIALLY_CREDITED" : invoice.getStatus()));
        invoiceRepository.save(invoice);
        return get(id);
    }

    public Map<String, Object> get(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT cn.*, i.invoice_no, p.number AS partner_no,
                       TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) AS partner_name
                  FROM credit_note cn
                  JOIN invoice i ON i.id = cn.invoice_id
                  LEFT JOIN partner p ON p.id = cn.partner_id
                 WHERE cn.id = ?
                """, id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Credit note not found: " + id);
        return new LinkedHashMap<>(rows.get(0));
    }

    public List<Map<String, Object>> findByInvoice(String invoiceId) {
        return jdbcTemplate.queryForList("""
                SELECT id, credit_note_no, invoice_id, partner_id, credit_note_date, reason,
                       total_cents, currency, status, created_at
                  FROM credit_note
                 WHERE invoice_id = ?
                 ORDER BY credit_note_date DESC, created_at DESC
                """, invoiceId);
    }

    public byte[] generatePdf(String id) {
        Map<String, Object> note = get(id);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document document = new Document(pdf)) {
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            companyPdfBrandingService.addITextHeader(document, regular, bold);
            document.add(new Paragraph("CREDIT NOTE").setFont(bold).setFontSize(20)
                    .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(8));
            document.add(new Paragraph("Credit Note Number: " + note.get("credit_note_no")).setFont(bold).setFontSize(10));
            document.add(new Paragraph("Date: " + note.get("credit_note_date")).setFont(regular).setFontSize(9));
            document.add(new Paragraph("Customer: " + defaultString(note.get("partner_name"), "")).setFont(regular).setFontSize(9));
            document.add(new Paragraph("Original Invoice: " + note.get("invoice_no")).setFont(regular).setFontSize(9).setMarginBottom(14));
            Table lines = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
            lines.addHeaderCell(cell("Description", bold, 9, TextAlignment.LEFT));
            lines.addHeaderCell(cell("Amount ZAR", bold, 9, TextAlignment.RIGHT));
            lines.addCell(cell(defaultString(note.get("reason"), "Credit adjustment"), regular, 9, TextAlignment.LEFT));
            lines.addCell(cell(formatCents(note.get("total_cents")), regular, 9, TextAlignment.RIGHT));
            document.add(lines);
            document.add(new Paragraph("TOTAL CREDIT ZAR  " + formatCents(note.get("total_cents")))
                    .setFont(bold).setFontSize(11).setTextAlignment(TextAlignment.RIGHT).setMarginTop(12));
            document.add(new Paragraph("This credit note reduces the balance of invoice " + note.get("invoice_no") + ".")
                    .setFont(regular).setFontSize(8).setMarginTop(16));
            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to generate credit note PDF", exception);
        }
    }

    private Cell cell(String text, PdfFont font, int size, TextAlignment alignment) {
        return new Cell().setBorder(Border.NO_BORDER).setTextAlignment(alignment)
                .add(new Paragraph(text).setFont(font).setFontSize(size));
    }

    private String generateNumber() {
        try {
            String allocated = numberAllocationService.allocateNumber("CREDIT_NOTE");
            return allocated.startsWith("CN") ? allocated : "CN-" + allocated;
        } catch (Exception ignored) {
            return "CN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        }
    }

    private long value(Long amount) { return amount == null ? 0L : amount; }
    private String defaultString(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
    private String formatCents(Object value) {
        long cents = value instanceof Number number ? number.longValue() : Long.parseLong(defaultString(value, "0"));
        return String.format(Locale.US, "%,.2f", cents / 100.0);
    }
}
