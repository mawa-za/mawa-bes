package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.funeral.FuneralClaimFundingReversalRequestDto;
import za.co.mawa.bes.service.UserAccessService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuneralClaimFundingReversalServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock UserAccessService access;
    @InjectMocks FuneralClaimFundingReversalService service;

    @BeforeEach
    void tenant() {
        TenantContext.setCurrentTenant("provider");
        UserContext.setCurrentUserId("finance-user");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    void invalidReasonCannotStartReversal() {
        FuneralClaimFundingReversalRequestDto request = request("short", "REF-123", null);
        assertThrows(IllegalArgumentException.class,
                () -> service.reconcile("fsr-1", "claim-1", request));
        verifyNoInteractions(jdbc);
    }

    @Test
    void cannotReconcileClaimNotLinkedToProvider() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("fsr-1")))
                .thenReturn(List.of("fsr-1"));
        when(jdbc.queryForList(contains("FROM funeral_service_claim WHERE"), eq("fsr-1"), eq("claim-1")))
                .thenReturn(List.of());
        assertThrows(IllegalArgumentException.class,
                () -> service.reconcile("fsr-1", "claim-1", request("Package upgrade for two covers", "REF-123", null)));
    }

    @Test
    void secondAttemptReturnsOriginalReversalWithoutAllocatingAnotherDeposit() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("fsr-1")))
                .thenReturn(List.of("fsr-1"));
        when(jdbc.queryForList(contains("FROM funeral_service_claim WHERE"), eq("fsr-1"), eq("claim-1")))
                .thenReturn(List.of(Map.of("id", "link-1", "claim_storage_scope", "LOCAL")));
        when(jdbc.queryForList(contains("`funeral_claim_funding_reversal`"), eq("claim-1")))
                .thenReturn(List.of(Map.of("original_claim_id", "claim-1", "paid_amount_cents", 25000L)));
        var result = service.reconcile("fsr-1", "claim-1",
                request("Same upgrade retried", "BANK-REC-71", 71L));
        assertEquals(25000L, result.get("paid_amount_cents"));
        verify(jdbc, never()).queryForList(contains("`bank_transaction`"), any());
    }

    @Test
    void insufficientUnallocatedBankCreditCannotReconcile() {
        claim("PAID");
        zeroApprovals();
        payments(List.of(Map.of("id", "pay-1", "status", "PAID",
                "amount", new BigDecimal("250.00"), "fnb_instruction_id", "FNB-123")));
        when(jdbc.queryForList(contains("`bank_transaction`"), eq(71L)))
                .thenReturn(List.of(Map.of("id", 71L, "amount_cents", 25000L,
                        "transaction_date", LocalDate.of(2026, 11, 20))));
        when(jdbc.queryForObject(contains("`bank_transaction_allocation`"), eq(Long.class), eq(71L)))
                .thenReturn(5000L);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.reconcile("fsr-1", "claim-1", request("Refund verified by finance", "BANK-REC-71", 71L)));
        assertTrue(error.getMessage().contains("Unallocated"));
    }

    @Test
    void pendingClaimApprovalMustBeWithdrawnBeforeReversal() {
        claim("APPROVED");
        when(jdbc.queryForObject(contains("approval_request"), eq(Long.class), eq("claim-1")))
                .thenReturn(1L);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.reconcile("fsr-1", "claim-1", request("Upgrade to two covers", "REF-123", null)));
        assertTrue(error.getMessage().contains("approval"));
    }

    @Test
    void approvedClaimWithUnsentPaymentInstructionCannotBeReissued() {
        claim("APPROVED");
        zeroApprovals();
        payments(List.of(Map.of("id", "pay-1", "status", "QUEUED_FOR_PAYMENT",
                "amount", new BigDecimal("250.00"), "fnb_instruction_id", "")));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.reconcile("fsr-1", "claim-1", request("Package upgrade for two covers", "REF-123", null)));
        assertTrue(error.getMessage().contains("QUEUED_FOR_PAYMENT"));
    }

    @Test
    void paidClaimRequiresBankImportRecoveryNotFreeTextAttestation() {
        claim("PAID");
        zeroApprovals();
        payments(List.of(Map.of("id", "pay-1", "status", "PAID",
                "amount", new BigDecimal("250.00"), "fnb_instruction_id", "FNB-123",
                "paid_date", LocalDate.of(2026, 11, 19))));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.reconcile("fsr-1", "claim-1", request("Upgrade to two covers", "REF-123", null)));
        assertTrue(error.getMessage().contains("imported recovery"));
    }

    @Test
    void financeReconciledPaidClaimPreservesItsPaidStatusAndAllocatesActualDeposit() {
        claim("PAID");
        zeroApprovals();
        payments(List.of(Map.of("id", "pay-1", "status", "PAID",
                "amount", new BigDecimal("250.00"), "fnb_instruction_id", "FNB-123",
                "paid_date", LocalDate.of(2026, 11, 19))));
        when(jdbc.queryForList(contains("`bank_transaction`"), eq(71L)))
                .thenReturn(List.of(Map.of("id", 71L, "amount_cents", 25000L,
                        "transaction_date", LocalDate.of(2026, 11, 20))));
        when(jdbc.queryForObject(contains("`bank_transaction_allocation`"), eq(Long.class), eq(71L)))
                .thenReturn(0L);
        when(jdbc.queryForMap(contains("`funeral_claim_funding_reversal`"), eq("claim-1")))
                .thenReturn(Map.of("original_claim_id", "claim-1", "paid_amount_cents", 25000L));

        Map<String, Object> record = service.reconcile("fsr-1", "claim-1",
                request("Additional cover approved", "BANK-REC-71", 71L));

        assertEquals(25000L, record.get("paid_amount_cents"));
        verify(jdbc).update(contains("'FUNERAL_CLAIM_RECOVERY'"),
                eq(71L), eq("claim-1"), eq(25000L), eq("finance-user"));
        verify(jdbc).update(contains("UPDATE funeral_service_claim SET superseded_at"), eq("link-1"));
        verify(jdbc, never()).update(contains("SET status='CANCELLED'"), any(), any(), any());
    }

    @Test
    void approvedButUnpaidClaimIsCancelledWithoutRecordingARefund() {
        claim("APPROVED");
        zeroApprovals();
        payments(List.of());
        when(jdbc.queryForMap(contains("`funeral_claim_funding_reversal`"), eq("claim-1")))
                .thenReturn(Map.of("original_claim_id", "claim-1", "paid_amount_cents", 0L));
        service.reconcile("fsr-1", "claim-1",
                request("Reallocating to two covers", "APPROVAL-REV-1", null));
        verify(jdbc).update(contains("SET status='CANCELLED'"), anyString(),
                eq("finance-user"), eq("claim-1"));
        verify(jdbc).update(contains("UPDATE funeral_service_claim SET superseded_at"), eq("link-1"));
    }

    private void claim(String status) {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("fsr-1")))
                .thenReturn(List.of("fsr-1"));
        when(jdbc.queryForList(contains("FROM funeral_service_claim WHERE"), eq("fsr-1"), eq("claim-1")))
                .thenReturn(List.of(Map.of("id", "link-1", "claim_storage_scope", "LOCAL",
                        "service_payment_request_id", "pay-1")));
        when(jdbc.queryForList(contains("`funeral_claim_funding_reversal`"), eq("claim-1")))
                .thenReturn(List.of());
        when(jdbc.queryForList(contains("`membership_claim`"), eq("claim-1")))
                .thenReturn(List.of(Map.of("id", "claim-1", "claim_type", "FUNERAL",
                        "status", status, "claim_amount_cents", 25000L,
                        "funeral_service_id", "fsr-1", "funeral_provider_tenant_id", "provider")));
    }
    private void zeroApprovals() {
        when(jdbc.queryForObject(contains("approval_request"), eq(Long.class), eq("claim-1")))
                .thenReturn(0L);
    }
    private void payments(List<Map<String, Object>> rows) {
        when(jdbc.queryForList(contains("`payment_request`"), eq("claim-1"), eq("pay-1")))
                .thenReturn(rows);
    }
    private FuneralClaimFundingReversalRequestDto request(String reason, String reference, Long bankTransactionId) {
        FuneralClaimFundingReversalRequestDto request = new FuneralClaimFundingReversalRequestDto();
        request.setReason(reason);
        request.setReference(reference);
        request.setRecoveryBankTransactionId(bankTransactionId);
        return request;
    }
}
