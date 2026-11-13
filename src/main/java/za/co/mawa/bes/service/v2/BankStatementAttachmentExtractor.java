package za.co.mawa.bes.service.v2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Extract only CSV files from a direct bank attachment or a bounded ZIP; never write ZIP files to disk. */
final class BankStatementAttachmentExtractor {
    private static final int MAX_FILE_BYTES = 5_000_000;
    private static final int MAX_ZIP_ENTRIES = 12;

    private BankStatementAttachmentExtractor() { }

    record CsvFile(String fileName, String identitySuffix, byte[] bytes) { }

    static boolean supported(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".csv") || lower.endsWith(".zip");
    }

    static List<CsvFile> extract(String fileName, byte[] bytes) {
        if (!supported(fileName)) return List.of();
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Invalid bank statement attachment size");
        }
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return List.of(new CsvFile(fileName, "", bytes));
        }
        List<CsvFile> files = new ArrayList<>();
        int entryCount = 0;
        int totalUncompressed = 0;
        try (ZipInputStream archive = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException("Too many files in bank statement ZIP");
                }
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\', '/');
                name = name.substring(name.lastIndexOf('/') + 1);
                boolean isCsv = name.toLowerCase(Locale.ROOT).endsWith(".csv") && name.length() <= 250;
                ByteArrayOutputStream csv = isCsv ? new ByteArrayOutputStream() : null;
                byte[] buffer = new byte[8192];
                int length;
                // Bound every entry, including ignored files (otherwise a ZIP bomb in
                // notes.txt could be decompressed without accounting for its size).
                while ((length = archive.read(buffer)) != -1) {
                    totalUncompressed += length;
                    if (totalUncompressed > MAX_FILE_BYTES)
                        throw new IllegalArgumentException("Bank statement ZIP exceeds maximum extracted size");
                    if (isCsv) csv.write(buffer, 0, length);
                }
                if (isCsv && csv.size() > 0) files.add(new CsvFile(name, "zip:" + entryCount, csv.toByteArray()));
                archive.closeEntry();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid bank statement ZIP", ex);
        }
        return files;
    }

    static String attachmentIdentity(String originalId, CsvFile file) {
        if (file.identitySuffix().isBlank()) return originalId;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (originalId + ":" + file.identitySuffix()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
