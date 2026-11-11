package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import za.co.mawa.bes.entity.v2.BankStatementFileEntity;
import za.co.mawa.bes.entity.v2.BankStatementFileAttemptEntity;
import za.co.mawa.bes.repository.v2.BankStatementFileRepository;
import za.co.mawa.bes.repository.v2.BankStatementFileAttemptRepository;
import za.co.mawa.bes.service.AttachmentStorageService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BankStatementFileServiceTest {
    private final BankStatementFileRepository files = mock(BankStatementFileRepository.class);
    private final BankStatementFileAttemptRepository attempts = mock(BankStatementFileAttemptRepository.class);
    private final AttachmentStorageService storage = mock(AttachmentStorageService.class);
    private final BankTransactionService bank = mock(BankTransactionService.class);
    private final BankStatementMailConfigurationService config = mock(BankStatementMailConfigurationService.class);
    private final BankStatementFileService service = new BankStatementFileService(files, attempts, storage, bank, config);

    private static byte[] csv() { return "example".getBytes(StandardCharsets.UTF_8); }

    @Test
    void manualUploadArchivesOriginalWithTenantStorageServiceBeforeProcessing() {
        when(files.saveAndFlush(any(BankStatementFileEntity.class))).thenAnswer(call -> {
            BankStatementFileEntity f = call.getArgument(0);
            f.setId(17L);
            return f;
        });
        when(storage.store(any(byte[].class), eq("csv"), eq("bank-statements"), eq("17"),
                eq("original"), eq("statement.csv"))).thenReturn(
                    new AttachmentStorageService.StoredAttachment("GCP", "private-bucket", "tenant/17/original.csv", "text/csv", 7L));

        var outcome = service.manualUpload("statement.csv", csv(), "auditor-1");

        assertFalse(outcome.alreadyReceived());
        assertEquals("STORED", outcome.file().status());
        assertEquals("MANUAL", outcome.file().source());
        assertTrue(outcome.file().stored());
        verifyNoInteractions(bank);
        verify(storage).store(any(byte[].class), eq("csv"), eq("bank-statements"), eq("17"),
                eq("original"), eq("statement.csv"));
    }

    @Test
    void previouslyArchivedEmailIsNotUploadedAgain() {
        BankStatementFileEntity original = file();
        when(files.findBySourceAndProviderAndMessageIdAndAttachmentId("EMAIL", "GMAIL", "message1", "attachment1"))
                .thenReturn(Optional.of(original));

        var outcome = service.emailAttachment("GMAIL", "message1", "attachment1", "statement.csv", csv());

        assertTrue(outcome.alreadyReceived());
        assertEquals(17L, outcome.file().id());
        verifyNoInteractions(storage, bank);
    }

    @Test
    void replayImportsFromArchivedBytesAndRecordsTheAttempt() {
        BankStatementFileEntity file = file();
        file.setAttemptCount(1);
        when(files.claim(eq(17L), any(), any())).thenReturn(1);
        when(files.findById(17L)).thenReturn(Optional.of(file));
        when(attempts.findByFileIdOrderByStartedAtDescIdDesc(17L)).thenReturn(List.of());
        when(attempts.saveAndFlush(any(BankStatementFileAttemptEntity.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(files.saveAndFlush(any(BankStatementFileEntity.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(storage.read("private-bucket", "tenant/17/original.csv")).thenReturn(csv());
        when(config.get()).thenReturn(new BankStatementMailConfigurationService.Configuration(
                false, null, null, null, null, null, null, null, null,
                14, null, "000123", true, null, null, null, null));
        when(bank.importCsv(eq("MANUAL"), eq("manual-file-17"), eq("manual-file-17"),
                eq("statement.csv"), any(byte[].class), eq("000123"), eq(17L))).thenReturn(2);

        var outcome = service.process(17L, "auditor-1");

        assertEquals("COMPLETED", outcome.status());
        assertEquals(2, outcome.importedCount());
        assertEquals(1, outcome.csvCount());
        verify(bank).importCsv(eq("MANUAL"), anyString(), anyString(), eq("statement.csv"),
                any(byte[].class), eq("000123"), eq(17L));
        verify(attempts, times(2)).saveAndFlush(any(BankStatementFileAttemptEntity.class));
    }

    @Test
    void aTamperedGcsObjectNeverReachesBankTransactionImport() {
        BankStatementFileEntity file = file();
        file.setAttemptCount(1);
        when(files.claim(eq(17L), any(), any())).thenReturn(1);
        when(files.findById(17L)).thenReturn(Optional.of(file));
        when(attempts.findByFileIdOrderByStartedAtDescIdDesc(17L)).thenReturn(List.of());
        when(attempts.saveAndFlush(any(BankStatementFileAttemptEntity.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(files.saveAndFlush(any(BankStatementFileEntity.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(storage.read("private-bucket", "tenant/17/original.csv"))
                .thenReturn("tampered!".getBytes(StandardCharsets.UTF_8));

        var result = service.process(17L, "auditor-1");

        assertEquals("FAILED", result.status());
        assertTrue(result.lastError().contains("integrity"));
        verifyNoInteractions(bank);
    }

    private static BankStatementFileEntity file() {
        BankStatementFileEntity f = new BankStatementFileEntity();
        f.setId(17L);
        f.setSource("MANUAL"); f.setProvider("MANUAL");
        f.setFileName("statement.csv");
        f.setSha256(BankStatementCsvParser.sha256(csv()));
        f.setByteSize((long) csv().length);
        f.setStatus("STORED");
        f.setUploadedBy("auditor-1");
        f.setStorageBucket("private-bucket");
        f.setStoragePath("tenant/17/original.csv");
        return f;
    }
}
