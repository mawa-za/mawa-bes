package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.entity.v2.CashupDepositEntity;
import za.co.mawa.bes.repository.v2.CashupDepositRepository;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import za.co.mawa.bes.repository.v2.CashupRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashupApprovalHandlerTest {

    @Mock private CashupRepository cashupRepository;
    @Mock private CashupDepositRepository deposits;
    @Mock private BankDepositVerificationPolicyService depositPolicy;
    @InjectMocks private CashupApprovalHandler handler;

    @Test
    void approvalRejectionReturnsCashupToAwaitingDeposits() {
        CashupEntity cashup = CashupEntity.builder()
                .id("cashup-1")
                .status("SUBMITTED")
                .approvalRequestId("approval-1")
                .build();
        ApprovalRequestEntity approval = ApprovalRequestEntity.builder()
                .id("approval-1")
                .referenceId("cashup-1")
                .build();
        when(cashupRepository.findById("cashup-1")).thenReturn(Optional.of(cashup));

        handler.onRejected(approval, "approver-1", "Attach the correct deposit proof");

        assertEquals("AWAITING_DEPOSITS", cashup.getStatus());
        assertNull(cashup.getApprovalRequestId());
        assertEquals("Attach the correct deposit proof", cashup.getNotes());
        assertEquals("approver-1", cashup.getUpdatedBy());
        verify(cashupRepository).save(cashup);
    }

    @Test
    void oldUnverifiedDepositCanBeApprovedDuringTransition() {
        CashupEntity cashup = CashupEntity.builder().id("cashup-legacy")
                .source("MAWA_PAY_CARD").status("SUBMITTED").build();
        CashupDepositEntity deposit = new CashupDepositEntity();
        deposit.setId("deposit-legacy");
        deposit.setCreatedBy("cashier");
        LocalDateTime captureTime = LocalDateTime.of(2026, 11, 1, 12, 0);
        // createdAt comes from the database and is read-only in JPA; set via reflection for the test.
        org.springframework.test.util.ReflectionTestUtils.setField(deposit, "createdAt", captureTime);
        ApprovalRequestEntity approval = ApprovalRequestEntity.builder().referenceId("cashup-legacy").build();
        when(cashupRepository.findById("cashup-legacy")).thenReturn(Optional.of(cashup));
        when(deposits.findByCashupIdOrderByDepositDateDescCreatedAtDesc("cashup-legacy"))
                .thenReturn(List.of(deposit));
        when(depositPolicy.requiredAt(captureTime)).thenReturn(false);
        handler.onApproved(approval, "approver");
        assertEquals("APPROVED", cashup.getStatus());
        verify(cashupRepository).save(cashup);
    }

    @Test
    void newlyCapturedUnverifiedDepositCannotBeApprovedAfterEnforcement() {
        CashupEntity cashup = CashupEntity.builder().id("cashup-new")
                .source("MAWA_PAY_CARD").status("SUBMITTED").build();
        CashupDepositEntity deposit = new CashupDepositEntity();
        LocalDateTime captureTime = LocalDateTime.of(2026, 11, 30, 12, 0);
        org.springframework.test.util.ReflectionTestUtils.setField(deposit, "createdAt", captureTime);
        ApprovalRequestEntity approval = ApprovalRequestEntity.builder().referenceId("cashup-new").build();
        when(cashupRepository.findById("cashup-new")).thenReturn(Optional.of(cashup));
        when(deposits.findByCashupIdOrderByDepositDateDescCreatedAtDesc("cashup-new"))
                .thenReturn(List.of(deposit));
        when(depositPolicy.requiredAt(captureTime)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> handler.onApproved(approval, "approver"));
    }
}
