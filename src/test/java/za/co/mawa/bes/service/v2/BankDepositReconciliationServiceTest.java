package za.co.mawa.bes.service.v2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.v2.CashupDepositEntity;
import za.co.mawa.bes.entity.v2.PaymentBatchEntity;
import za.co.mawa.bes.enums.PaymentBatchStatus;
import za.co.mawa.bes.repository.v2.CashupDepositRepository;
import za.co.mawa.bes.repository.v2.PaymentBatchRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BankDepositReconciliationServiceTest {
    private final PaymentBatchRepository batches = mock(PaymentBatchRepository.class);
    private final CashupDepositRepository deposits = mock(CashupDepositRepository.class);
    private final BankTransactionService bank = mock(BankTransactionService.class);
    private final EntityManager em = mock(EntityManager.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final BankDepositReconciliationService service = new BankDepositReconciliationService(batches, deposits, bank, em, jdbc);

    @AfterEach void cleanup() { UserContext.clear(); }

    @Test void linksPreviouslyPostedEftWithoutChangingStatusOrAmount() {
        UserContext.setCurrentUserId("auditor-1");
        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setId("batch-1"); batch.setPaymentMethod("EFT");
        batch.setTotalAmountCents(25000L); batch.setStatus(PaymentBatchStatus.POSTED);
        when(em.find(PaymentBatchEntity.class, "batch-1", LockModeType.PESSIMISTIC_WRITE)).thenReturn(batch);
        when(bank.allocate("DEP-00000451", 25000L, "PAYMENT_BATCH", "batch-1", "auditor-1")).thenReturn(451L);
        var result = service.reconcile(new BankDepositReconciliationService.ReconcileRequest("PAYMENT_BATCH", "batch-1", "DEP-00000451"));
        assertEquals("BANK_VERIFIED", result.status());
        assertEquals(451L, batch.getBankDepositId());
        assertEquals(PaymentBatchStatus.POSTED, batch.getStatus());
        assertEquals(25000L, batch.getTotalAmountCents());
        verify(em).flush();
    }

    @Test void cannotAllocateSameEftTwice() {
        UserContext.setCurrentUserId("auditor-1");
        PaymentBatchEntity batch = new PaymentBatchEntity();
        batch.setId("batch-2"); batch.setPaymentMethod("EFT");
        batch.setTotalAmountCents(10000L); batch.setStatus(PaymentBatchStatus.POSTED);
        batch.setBankDepositId(12L);
        when(em.find(PaymentBatchEntity.class, "batch-2", LockModeType.PESSIMISTIC_WRITE)).thenReturn(batch);
        assertThrows(IllegalStateException.class,
                () -> service.reconcile(new BankDepositReconciliationService.ReconcileRequest("PAYMENT_BATCH", "batch-2", "DEP-00000451")));
        verifyNoInteractions(bank);
    }

    @Test void approvedCashupCanBeLinkedWithoutReapproval() {
        UserContext.setCurrentUserId("auditor-1");
        CashupDepositEntity deposit = new CashupDepositEntity();
        deposit.setId("cashup-deposit-1"); deposit.setAmountCents(59000L);
        when(em.find(CashupDepositEntity.class, "cashup-deposit-1", LockModeType.PESSIMISTIC_WRITE)).thenReturn(deposit);
        when(bank.allocate("DEP-00000555", 59000L, "CASHUP_DEPOSIT", "cashup-deposit-1", "auditor-1")).thenReturn(555L);
        var result = service.reconcile(new BankDepositReconciliationService.ReconcileRequest("CASHUP_DEPOSIT", "cashup-deposit-1", "DEP-00000555"));
        assertEquals("BANK_VERIFIED", result.status());
        assertEquals(555L, deposit.getBankDepositId());
        verify(em).flush();
    }
}
