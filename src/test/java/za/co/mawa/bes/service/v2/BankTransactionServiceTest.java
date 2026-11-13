package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import za.co.mawa.bes.entity.v2.BankTransactionEntity;
import za.co.mawa.bes.repository.v2.BankStatementImportRepository;
import za.co.mawa.bes.repository.v2.BankTransactionAllocationRepository;
import za.co.mawa.bes.repository.v2.BankTransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BankTransactionServiceTest {
    private final BankStatementCsvParser parser = mock(BankStatementCsvParser.class);
    private final BankStatementImportRepository imports = mock(BankStatementImportRepository.class);
    private final BankTransactionRepository transactions = mock(BankTransactionRepository.class);
    private final BankTransactionAllocationRepository allocations = mock(BankTransactionAllocationRepository.class);
    private final BankTransactionService service = new BankTransactionService(parser, imports, transactions, allocations);
    private final PageRequest firstPage = PageRequest.of(0, 20);

    private BankTransactionEntity transaction(long id, long amountCents) {
        BankTransactionEntity entity = new BankTransactionEntity();
        entity.setId(id);
        entity.setAccountNumber("00000000000012345678");
        entity.setReferenceText(amountCents > 0 ? "PREMIUM PAYMENT" : "CLAIM PAYOUT");
        entity.setTransactionDate(LocalDate.of(2026, 11, 12));
        entity.setAmountCents(amountCents);
        return entity;
    }

    @Test
    void mixedHistoryPreservesCreditDepositIdsAndUsesTransactionIdsForDebitsAndZeroes() {
        var credit = transaction(123L, 100_00L);
        var debit = transaction(124L, -250_00L);
        var zero = transaction(125L, 0L);
        when(transactions.search("", false, null, null, null, firstPage))
                .thenReturn(new PageImpl<>(List.of(credit, debit, zero), firstPage, 3));

        var bankRows = service.search("", false, null, null, null, firstPage).getContent();
        assertEquals(3, bankRows.size());
        assertEquals("DEP-00000123", bankRows.get(0).depositId());
        assertEquals("TXN-00000123", bankRows.get(0).transactionId());
        assertEquals("AVAILABLE", bankRows.get(0).status());
        assertNull(bankRows.get(1).depositId());
        assertEquals("TXN-00000124", bankRows.get(1).transactionId());
        assertEquals("DEBIT", bankRows.get(1).status());
        assertEquals(0L, bankRows.get(1).availableCents());
        assertNull(bankRows.get(2).depositId());
        assertEquals("TXN-00000125", bankRows.get(2).transactionId());
        assertEquals("DEBIT", bankRows.get(2).status());
    }

    @Test
    void searchByDepositIdOnlyResolvesCreditsAndTransactionIdCanResolveDebit() {
        var debit = transaction(124L, -250_00L);
        when(transactions.searchById(124L, false, null, null, null, firstPage))
                .thenReturn(new PageImpl<>(List.of(debit), firstPage, 1));
        when(transactions.searchById(124L, true, null, null, null, firstPage))
                .thenReturn(new PageImpl<>(List.of(), firstPage, 0));

        assertEquals("TXN-00000124", service.search("txn-00000124", false, null, null, null,
                firstPage).getContent().get(0).transactionId());
        assertTrue(service.search("DEP-00000124", false, null, null, null, firstPage).isEmpty());
        verify(transactions).searchById(124L, true, null, null, null, firstPage);
        verify(transactions).searchById(124L, false, null, null, null, firstPage);
    }

    @Test
    void getRejectsForgedDepositReferenceForDebitButAcceptsTransactionReference() {
        var debit = transaction(124L, -250_00L);
        when(transactions.findById(124L)).thenReturn(Optional.of(debit));
        assertThrows(IllegalArgumentException.class, () -> service.get("DEP-00000124"));
        var result = service.get("TXN-00000124");
        assertNull(result.depositId());
        assertEquals("TXN-00000124", result.transactionId());
    }

    @Test
    void depositAllocationAndHistoryRejectDebitAndNeutralTransactionReferences() {
        var debit = transaction(124L, -250_00L);
        when(transactions.findLocked(124L)).thenReturn(Optional.of(debit));
        when(transactions.findById(124L)).thenReturn(Optional.of(debit));
        assertThrows(IllegalArgumentException.class,
                () -> service.allocate("DEP-00000124", 100L, "PAYMENT_BATCH", "pb-1", "tester"));
        assertThrows(IllegalArgumentException.class, () -> service.allocations("DEP-00000124"));
        assertThrows(IllegalArgumentException.class,
                () -> service.allocate("TXN-00000124", 100L, "PAYMENT_BATCH", "pb-1", "tester"));
        verifyNoInteractions(allocations);
    }

    @Test
    void legacyCreditIdsRemainStableAndExistingAllocationBalancesAreUnaffected() {
        var credit = transaction(123L, 100_00L);
        when(transactions.findById(123L)).thenReturn(Optional.of(credit));
        when(allocations.usedAmount(123L)).thenReturn(40_00L);
        var result = service.get("DEP-00000123");
        assertEquals("DEP-00000123", result.depositId());
        assertEquals("TXN-00000123", result.transactionId());
        assertEquals(40_00L, result.allocatedCents());
        assertEquals(60_00L, result.availableCents());
        verify(transactions, never()).save(any());
    }

    @Test
    void invalidDepositAndTransactionIdFormatsAreRejected() {
        assertEquals(123L, BankTransactionService.parseId("dep-00000123"));
        assertEquals("TXN-00000123", BankTransactionService.formatTransactionId(123L));
        assertThrows(IllegalArgumentException.class, () -> BankTransactionService.parseId("TXN-00000123"));
        assertThrows(IllegalArgumentException.class, () -> service.get("OTHER-00000123"));
        assertThrows(IllegalArgumentException.class,
                () -> service.search("TXN-notanumber", false, null, null, null, firstPage));
    }
}
