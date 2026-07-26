package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.PayrollPaymentBatchEntity;
import za.co.mawa.bes.entity.v2.PayrollPaymentItemEntity;
import za.co.mawa.bes.repository.v2.PayrollPaymentBatchRepository;
import za.co.mawa.bes.repository.v2.PayrollPaymentItemRepository;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollBatchPrintoutService {
    private final PayrollPaymentBatchRepository batchRepository;
    private final PayrollPaymentItemRepository itemRepository;
    private final PaymentAccountConfigurationService paymentAccountService;
    private final JdbcTemplate jdbcTemplate;

    public byte[] generate(String batchId) {
        PayrollPaymentBatchEntity batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll batch not found: " + batchId));
        List<PayrollPaymentItemEntity> items = itemRepository.findByBatchIdAndExcludedFalseOrderByEmployeeNameAsc(batchId);
        Map<String, Object> debtor = resolveDebtor(batch);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = new PDPage(landscapeA4());
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = 560;
            text(content, bold, 17, 35, y, "Payroll Payment Verification");
            y -= 22;
            text(content, regular, 9, 35, y, "Batch: " + safe(batch.getBatchNo())
                    + "    Period: " + safe(batch.getPayPeriod())
                    + "    Payment date: " + batch.getPaymentDate()
                    + "    Status: " + batch.getStatus());
            y -= 15;
            text(content, regular, 9, 35, y, "Debtor account: " + value(debtor, "account_holder")
                    + " | " + value(debtor, "bank_name")
                    + " | " + value(debtor, "account_number")
                    + " | " + value(debtor, "account_type")
                    + " | Universal branch " + value(debtor, "branch_code"));
            y -= 15;
            text(content, regular, 8, 35, y, "Generated: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            y -= 22;
            header(content, bold, y);
            y -= 16;
            int number = 1;
            long total = 0L;
            for (PayrollPaymentItemEntity item : items) {
                if (y < 45) {
                    content.close();
                    page = new PDPage(landscapeA4());
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = 560;
                    header(content, bold, y);
                    y -= 16;
                }
                total += item.getAmountCents() == null ? 0 : item.getAmountCents();
                row(content, regular, y, number++, item);
                y -= 15;
            }
            y -= 8;
            text(content, bold, 10, 600, y, "Total: R " + BigDecimal.valueOf(total, 2).toPlainString());
            y -= 24;
            text(content, regular, 9, 35, y, "Verified by: ______________________________    Date: ______________");
            text(content, regular, 9, 430, y, "Approved by: ______________________________    Date: ______________");
            content.close();
            document.save(out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate payroll verification printout", exception);
        }
    }

    private PDRectangle landscapeA4() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }

    private Map<String, Object> resolveDebtor(PayrollPaymentBatchEntity batch) {
        if (batch.getDebtorAccountId() != null && !batch.getDebtorAccountId().isBlank()) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM payment_bank_account WHERE id = ?", batch.getDebtorAccountId());
            if (!rows.isEmpty()) return rows.get(0);
        }
        return paymentAccountService.activePayrollDebtor().orElseThrow(() ->
                new IllegalStateException(
                        "Maintain an active PAYROLL_DEBTOR account before generating the payroll verification printout"));
    }

    private void header(PDPageContentStream content, PDFont font, float y) throws Exception {
        text(content, font, 7, 35, y, "#");
        text(content, font, 7, 52, y, "Employee");
        text(content, font, 7, 182, y, "Employee No");
        text(content, font, 7, 245, y, "Account Holder");
        text(content, font, 7, 365, y, "Bank");
        text(content, font, 7, 430, y, "Account Number");
        text(content, font, 7, 515, y, "Type");
        text(content, font, 7, 565, y, "Universal Branch");
        text(content, font, 7, 655, y, "Reference");
        text(content, font, 7, 760, y, "Amount");
    }

    private void row(PDPageContentStream content, PDFont font, float y, int number, PayrollPaymentItemEntity item) throws Exception {
        text(content, font, 7, 35, y, String.valueOf(number));
        text(content, font, 7, 52, y, limit(item.getEmployeeName(), 25));
        text(content, font, 7, 182, y, limit(item.getEmployeeNo(), 11));
        text(content, font, 7, 245, y, limit(item.getAccountHolderName(), 22));
        text(content, font, 7, 365, y, limit(item.getBankName(), 11));
        text(content, font, 7, 430, y, limit(item.getAccountNo(), 16));
        text(content, font, 7, 515, y, limit(item.getAccountType(), 8));
        text(content, font, 7, 565, y, safe(item.getBranchCode()));
        text(content, font, 7, 655, y, limit(item.getPaymentReference(), 18));
        long amountCents = item.getAmountCents() == null ? 0L : item.getAmountCents();
        text(content, font, 7, 760, y, "R " + BigDecimal.valueOf(amountCents, 2).toPlainString());
    }

    private void text(PDPageContentStream content, PDFont font, float size, float x, float y, String value) throws Exception {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(value).replace("\n", " "));
        content.endText();
    }

    private String value(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "Not configured" : value.toString();
    }
    private String safe(Object value) {
        if (value == null) return "";
        return value.toString().replaceAll("[^\\x20-\\x7E]", "?");
    }
    private String limit(String value, int max) {
        String safe = safe(value);
        if (safe.length() <= max) return safe;
        if (max <= 3) return safe.substring(0, max);
        return safe.substring(0, max - 3) + "...";
    }
}
