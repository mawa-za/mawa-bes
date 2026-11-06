package za.co.mawa.bes.service.v2;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;

/** Strict FNB ACCOUNT TRANSACTION HISTORY CSV parser. Never interpret mail body as financial data. */
@Component
public class BankStatementCsvParser {
    public record Transaction(String fingerprint, String accountNumber, LocalDate date,
                              long amountCents, long feeCents, String reference,
                              String chequeNumber, Long balanceCents) { }

    public List<Transaction> parse(byte[] content) {
        if (content == null || content.length == 0 || content.length > 5_000_000) {
            throw new IllegalArgumentException("Bank statement attachment must be between 1 byte and 5 MB");
        }
        String csv = new String(content, StandardCharsets.UTF_8).replace("\uFEFF", "");
        List<List<String>> records = csvRecords(csv);
        String account = null;
        int header = -1;
        for (int i = 0; i < Math.min(records.size(), 30); i++) {
            List<String> fields = records.get(i);
            if (fields.isEmpty()) continue;
            String first = fields.get(0).trim().toUpperCase(Locale.ROOT);
            if (first.startsWith("FOR ACCOUNT NUMBER")) {
                account = fields.get(0).substring("FOR ACCOUNT NUMBER".length()).replaceAll("[^0-9]", "");
            }
            if (fields.size() >= 6 && "DATE".equals(first)
                    && "AMOUNT".equalsIgnoreCase(fields.get(2).trim())
                    && "REFERENCE".equalsIgnoreCase(fields.get(3).trim())) {
                header = i;
            }
        }
        if (account == null || account.isBlank() || header < 0) {
            throw new IllegalArgumentException("Unsupported bank statement CSV: FNB account and column headings required");
        }
        Map<String, Integer> occurrences = new HashMap<>();
        List<Transaction> parsed = new ArrayList<>();
        for (int i = header + 1; i < records.size(); i++) {
            List<String> f = records.get(i);
            if (f.isEmpty() || f.get(0).isBlank()) continue;
            if (f.size() < 6) throw new IllegalArgumentException("Incomplete bank statement row " + (i + 1));
            LocalDate date;
            try { date = LocalDate.parse(f.get(0).trim()); }
            catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid transaction date in row " + (i + 1)); }
            long fee = cents(f.get(1));
            long amount = cents(f.get(2));
            String reference = f.get(3).trim();
            if (reference.isEmpty()) throw new IllegalArgumentException("Missing transaction reference in row " + (i + 1));
            if (reference.length() > 500) throw new IllegalArgumentException("Transaction reference is too long in row " + (i + 1));
            String cheque = f.get(4).trim();
            Long balance = f.get(5).isBlank() ? null : cents(f.get(5));
            String canonical = account + "|" + date + "|" + amount + "|" + fee + "|"
                    + reference + "|" + cheque + "|" + balance;
            int occurrence = occurrences.merge(canonical, 1, Integer::sum);
            parsed.add(new Transaction(sha256((canonical + "|" + occurrence).getBytes(StandardCharsets.UTF_8)),
                    account, date, amount, fee, reference, cheque, balance));
            if (parsed.size() > 100_000) throw new IllegalArgumentException("Bank statement exceeds 100,000 rows");
        }
        if (parsed.isEmpty()) throw new IllegalArgumentException("Bank statement contains no transaction rows");
        return parsed;
    }

    static long cents(String value) {
        try {
            return new BigDecimal(value.trim().replace(",", ""))
                    .movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid bank statement amount");
        }
    }

    static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    /** RFC 4180 CSV with escaped double quotes and quoted line breaks. */
    static List<List<String>> csvRecords(String csv) {
        List<List<String>> result = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') { value.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                fields.add(value.toString()); value.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                fields.add(value.toString()); value.setLength(0);
                result.add(fields); fields = new ArrayList<>();
            } else value.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unterminated quoted CSV field");
        if (!fields.isEmpty() || !value.isEmpty()) {
            fields.add(value.toString()); result.add(fields);
        }
        return result;
    }
}
