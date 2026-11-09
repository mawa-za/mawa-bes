package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class BankStatementAttachmentExtractorTest {
    @Test
    void extractsCsvFromZipAndKeepsDeterministicImportIdentity() throws Exception {
        byte[] zip = archive("statement.csv", "FOR ACCOUNT NUMBER000123\n", "notes.txt", "text");
        var files = BankStatementAttachmentExtractor.extract("fnb.zip", zip);
        assertEquals(1, files.size());
        assertEquals("statement.csv", files.get(0).fileName());
        assertTrue(new String(files.get(0).bytes(), StandardCharsets.UTF_8).startsWith("FOR ACCOUNT NUMBER"));
        String identity = BankStatementAttachmentExtractor.attachmentIdentity("graph-attachment", files.get(0));
        assertEquals(64, identity.length());
        assertEquals(identity, BankStatementAttachmentExtractor.attachmentIdentity("graph-attachment", files.get(0)));
    }

    @Test
    void directCsvKeepsExistingImportIdentityAndUnsupportedTypesAreExcluded() {
        byte[] content = "DATE,AMOUNT".getBytes(StandardCharsets.UTF_8);
        var files = BankStatementAttachmentExtractor.extract("statement.CSV", content);
        assertEquals(1, files.size());
        assertEquals("message-attachment-id", BankStatementAttachmentExtractor.attachmentIdentity("message-attachment-id", files.get(0)));
        assertFalse(BankStatementAttachmentExtractor.supported("fnb.pdf"));
        assertTrue(BankStatementAttachmentExtractor.extract("fnb.pdf", content).isEmpty());
    }

    @Test
    void nestedPathsAreNotExtractedToDisk() throws Exception {
        byte[] zip = archive("../nested/safe.csv", "CSV", "file.txt", "not imported");
        List<BankStatementAttachmentExtractor.CsvFile> files = BankStatementAttachmentExtractor.extract("statement.zip", zip);
        assertEquals("safe.csv", files.get(0).fileName());
    }

    @Test
    void zipWithExcessiveExtractedSizeIsRejected() throws Exception {
        byte[] zip = archive("large.csv", "a".repeat(5_000_001), "note.txt", "text");
        assertThrows(IllegalArgumentException.class, () -> BankStatementAttachmentExtractor.extract("statement.zip", zip));
    }

    private static byte[] archive(String name1, String value1, String name2, String value2) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (String[] file : new String[][]{{name1,value1},{name2,value2}}) {
                out.putNextEntry(new ZipEntry(file[0]));
                out.write(file[1].getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
