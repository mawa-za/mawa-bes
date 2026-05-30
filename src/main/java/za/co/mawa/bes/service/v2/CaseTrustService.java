package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.v2.CaseTrustLedgerEntity;
import za.co.mawa.bes.entity.v2.CaseTrustTransactionEntity;
import za.co.mawa.bes.entity.v2.LegalCaseEntity;
import za.co.mawa.bes.enums.CaseDisbursementType;
import za.co.mawa.bes.enums.TrustPaymentMethod;
import za.co.mawa.bes.enums.TrustTransactionDirection;
import za.co.mawa.bes.enums.TrustTransactionType;
import za.co.mawa.bes.repository.v2.CaseTrustLedgerRepository;
import za.co.mawa.bes.repository.v2.CaseTrustTransactionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseTrustService {

    private final LegalCaseService legalCaseService;
    private final CaseBillingService caseBillingService;
    private final CaseDisbursementService caseDisbursementService;
    private final CaseTrustLedgerRepository ledgerRepository;
    private final CaseTrustTransactionRepository transactionRepository;

    @Transactional
    public CaseTrustTransactionEntity receiveTrustFunds(String caseId, CaseTrustReceiptRequest request) {
        validatePositiveAmount(request.getAmountCents());

        LegalCaseEntity legalCase = legalCaseService.findById(caseId);
        CaseTrustLedgerEntity ledger = getOrCreateLedger(legalCase, request.getReceivedBy());

        Long newBalance = ledger.getAvailableBalanceCents() + request.getAmountCents();
        ledger.setTotalReceivedCents(ledger.getTotalReceivedCents() + request.getAmountCents());
        ledger.setAvailableBalanceCents(newBalance);
        ledger.setUpdatedAt(LocalDateTime.now());
        ledger.setUpdatedBy(request.getReceivedBy());
        ledgerRepository.save(ledger);

        return createTransaction(
                legalCase,
                TrustTransactionType.RECEIPT,
                TrustTransactionDirection.IN,
                request.getAmountCents(),
                newBalance,
                request.getPaymentMethod(),
                request.getReferenceNo(),
                request.getBankReference(),
                null,
                request.getDescription(),
                null,
                null,
                null,
                request.getReceivedBy()
        );
    }

    @Transactional
    public CaseTrustTransactionEntity transferToBusiness(String caseId, CaseTrustBusinessTransferRequest request) {
        validatePositiveAmount(request.getAmountCents());

        if (isBlank(request.getInvoiceId())) {
            throw new RuntimeException("Invoice ID is required for trust-to-business transfer");
        }

        LegalCaseEntity legalCase = legalCaseService.findById(caseId);
        CaseTrustLedgerEntity ledger = getExistingLedger(caseId);
        ensureSufficientFunds(ledger, request.getAmountCents());

        Long newBalance = ledger.getAvailableBalanceCents() - request.getAmountCents();
        ledger.setTotalTransferredCents(ledger.getTotalTransferredCents() + request.getAmountCents());
        ledger.setAvailableBalanceCents(newBalance);
        ledger.setUpdatedAt(LocalDateTime.now());
        ledger.setUpdatedBy(request.getTransferredBy());
        ledgerRepository.save(ledger);

        CaseTrustTransactionEntity transaction = createTransaction(
                legalCase,
                TrustTransactionType.TRANSFER_TO_BUSINESS,
                TrustTransactionDirection.OUT,
                request.getAmountCents(),
                newBalance,
                null,
                request.getInvoiceId(),
                null,
                null,
                request.getDescription(),
                request.getInvoiceId(),
                null,
                null,
                request.getTransferredBy()
        );

        caseBillingService.recalculateCaseTotals(caseId);
        return transaction;
    }

    @Transactional
    public CaseTrustTransactionEntity refundClient(String caseId, CaseTrustRefundRequest request) {
        validatePositiveAmount(request.getAmountCents());

        LegalCaseEntity legalCase = legalCaseService.findById(caseId);
        CaseTrustLedgerEntity ledger = getExistingLedger(caseId);
        ensureSufficientFunds(ledger, request.getAmountCents());

        Long newBalance = ledger.getAvailableBalanceCents() - request.getAmountCents();
        ledger.setTotalRefundedCents(ledger.getTotalRefundedCents() + request.getAmountCents());
        ledger.setAvailableBalanceCents(newBalance);
        ledger.setUpdatedAt(LocalDateTime.now());
        ledger.setUpdatedBy(request.getRefundedBy());
        ledgerRepository.save(ledger);

        return createTransaction(
                legalCase,
                TrustTransactionType.REFUND_TO_CLIENT,
                TrustTransactionDirection.OUT,
                request.getAmountCents(),
                newBalance,
                request.getPaymentMethod(),
                request.getReferenceNo(),
                request.getBankReference(),
                null,
                request.getDescription(),
                null,
                null,
                null,
                request.getRefundedBy()
        );
    }

    @Transactional
    public CaseTrustTransactionEntity payThirdParty(String caseId, CaseTrustThirdPartyPaymentRequest request) {
        validatePositiveAmount(request.getAmountCents());

        if (isBlank(request.getPayeeName())) {
            throw new RuntimeException("Payee name is required");
        }

        LegalCaseEntity legalCase = legalCaseService.findById(caseId);
        CaseTrustLedgerEntity ledger = getExistingLedger(caseId);
        ensureSufficientFunds(ledger, request.getAmountCents());

        Long newBalance = ledger.getAvailableBalanceCents() - request.getAmountCents();
        ledger.setTotalPaidOutCents(ledger.getTotalPaidOutCents() + request.getAmountCents());
        ledger.setAvailableBalanceCents(newBalance);
        ledger.setUpdatedAt(LocalDateTime.now());
        ledger.setUpdatedBy(request.getPaidBy());
        ledgerRepository.save(ledger);

        CaseTrustTransactionEntity transaction = createTransaction(
                legalCase,
                TrustTransactionType.PAYMENT_TO_THIRD_PARTY,
                TrustTransactionDirection.OUT,
                request.getAmountCents(),
                newBalance,
                request.getPaymentMethod(),
                request.getReferenceNo(),
                request.getBankReference(),
                request.getPayeeName(),
                request.getDescription(),
                null,
                null,
                null,
                request.getPaidBy()
        );

        if (Boolean.TRUE.equals(request.getCreateDisbursement())) {
            CaseDisbursementCreateRequest disbursementRequest = new CaseDisbursementCreateRequest();
            disbursementRequest.setDisbursementDate(LocalDate.now());
            disbursementRequest.setDisbursementType(request.getDisbursementType() != null ? request.getDisbursementType() : CaseDisbursementType.OTHER);
            disbursementRequest.setDescription(request.getDescription() != null ? request.getDescription() : "Trust payment to " + request.getPayeeName());
            disbursementRequest.setAmountCents(request.getAmountCents());
            disbursementRequest.setBillable(request.getBillableDisbursement() != null ? request.getBillableDisbursement() : true);
            disbursementRequest.setPaidFromTrust(true);
            disbursementRequest.setTrustTransactionId(transaction.getId());
            caseDisbursementService.create(caseId, disbursementRequest, request.getPaidBy());
        }

        return transaction;
    }

    public CaseTrustBalanceResponse getBalance(String caseId) {
        CaseTrustLedgerEntity ledger = ledgerRepository.findByCaseId(caseId).orElse(null);

        if (ledger == null) {
            LegalCaseEntity legalCase = legalCaseService.findById(caseId);
            return CaseTrustBalanceResponse.builder()
                    .caseId(caseId)
                    .clientPartnerId(legalCase.getClientPartnerId())
                    .currency("ZAR")
                    .totalReceivedCents(0L)
                    .totalTransferredCents(0L)
                    .totalRefundedCents(0L)
                    .totalPaidOutCents(0L)
                    .availableBalanceCents(0L)
                    .build();
        }

        return CaseTrustBalanceResponse.builder()
                .caseId(ledger.getCaseId())
                .clientPartnerId(ledger.getClientPartnerId())
                .currency(ledger.getCurrency())
                .totalReceivedCents(ledger.getTotalReceivedCents())
                .totalTransferredCents(ledger.getTotalTransferredCents())
                .totalRefundedCents(ledger.getTotalRefundedCents())
                .totalPaidOutCents(ledger.getTotalPaidOutCents())
                .availableBalanceCents(ledger.getAvailableBalanceCents())
                .build();
    }

    public List<CaseTrustTransactionEntity> getTransactions(String caseId) {
        legalCaseService.findById(caseId);
        return transactionRepository.findByCaseIdOrderByTransactionDateDesc(caseId);
    }

    @Transactional
    public CaseTrustTransactionEntity reverseTransaction(String caseId, String transactionId, CaseTrustReverseTransactionRequest request) {
        if (isBlank(request.getReason())) {
            throw new RuntimeException("Reversal reason is required");
        }

        LegalCaseEntity legalCase = legalCaseService.findById(caseId);

        CaseTrustTransactionEntity original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Trust transaction not found"));

        if (!original.getCaseId().equals(caseId)) {
            throw new RuntimeException("Transaction does not belong to this case");
        }

        if (Boolean.TRUE.equals(original.getReversed())) {
            throw new RuntimeException("Transaction already reversed");
        }

        if (original.getTransactionType() == TrustTransactionType.REVERSAL) {
            throw new RuntimeException("Reversal transactions cannot be reversed");
        }

        CaseTrustLedgerEntity ledger = getExistingLedger(caseId);
        Long reversalAmount = original.getAmountCents();
        Long newBalance;
        TrustTransactionDirection reversalDirection;

        if (original.getDirection() == TrustTransactionDirection.IN) {
            ensureSufficientFunds(ledger, reversalAmount);
            newBalance = ledger.getAvailableBalanceCents() - reversalAmount;
            reversalDirection = TrustTransactionDirection.OUT;
            ledger.setTotalReceivedCents(ledger.getTotalReceivedCents() - reversalAmount);
        } else {
            newBalance = ledger.getAvailableBalanceCents() + reversalAmount;
            reversalDirection = TrustTransactionDirection.IN;

            if (original.getTransactionType() == TrustTransactionType.TRANSFER_TO_BUSINESS) {
                ledger.setTotalTransferredCents(ledger.getTotalTransferredCents() - reversalAmount);
            } else if (original.getTransactionType() == TrustTransactionType.REFUND_TO_CLIENT) {
                ledger.setTotalRefundedCents(ledger.getTotalRefundedCents() - reversalAmount);
            } else if (original.getTransactionType() == TrustTransactionType.PAYMENT_TO_THIRD_PARTY) {
                ledger.setTotalPaidOutCents(ledger.getTotalPaidOutCents() - reversalAmount);
            }
        }

        ledger.setAvailableBalanceCents(newBalance);
        ledger.setUpdatedAt(LocalDateTime.now());
        ledger.setUpdatedBy(request.getReversedBy());
        ledgerRepository.save(ledger);

        original.setReversed(true);
        original.setReversedAt(LocalDateTime.now());
        original.setReversedBy(request.getReversedBy());
        original.setReversalReason(request.getReason());
        transactionRepository.save(original);

        CaseTrustTransactionEntity reversal = createTransaction(
                legalCase,
                TrustTransactionType.REVERSAL,
                reversalDirection,
                reversalAmount,
                newBalance,
                original.getPaymentMethod(),
                original.getReferenceNo(),
                original.getBankReference(),
                original.getPayeeName(),
                "Reversal: " + request.getReason(),
                original.getRelatedInvoiceId(),
                original.getRelatedReceiptId(),
                original.getId(),
                request.getReversedBy()
        );

        caseBillingService.recalculateCaseTotals(caseId);
        return reversal;
    }

    private CaseTrustLedgerEntity getOrCreateLedger(LegalCaseEntity legalCase, String createdBy) {
        return ledgerRepository.findByCaseId(legalCase.getId())
                .orElseGet(() -> {
                    CaseTrustLedgerEntity ledger = new CaseTrustLedgerEntity();
                    ledger.setCaseId(legalCase.getId());
                    ledger.setClientPartnerId(legalCase.getClientPartnerId());
                    ledger.setCurrency(legalCase.getCurrency() != null ? legalCase.getCurrency() : "ZAR");
                    ledger.setCreatedAt(LocalDateTime.now());
                    ledger.setCreatedBy(createdBy);
                    return ledgerRepository.save(ledger);
                });
    }

    private CaseTrustLedgerEntity getExistingLedger(String caseId) {
        return ledgerRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Trust ledger not found for case"));
    }

    private CaseTrustTransactionEntity createTransaction(
            LegalCaseEntity legalCase,
            TrustTransactionType type,
            TrustTransactionDirection direction,
            Long amountCents,
            Long balanceAfterCents,
            TrustPaymentMethod paymentMethod,
            String referenceNo,
            String bankReference,
            String payeeName,
            String description,
            String relatedInvoiceId,
            String relatedReceiptId,
            String relatedTransactionId,
            String createdBy
    ) {
        CaseTrustTransactionEntity transaction = new CaseTrustTransactionEntity();
        transaction.setCaseId(legalCase.getId());
        transaction.setClientPartnerId(legalCase.getClientPartnerId());
        transaction.setTransactionNo(generateTransactionNo());
        transaction.setTransactionType(type);
        transaction.setDirection(direction);
        transaction.setAmountCents(amountCents);
        transaction.setBalanceAfterCents(balanceAfterCents);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setReferenceNo(referenceNo);
        transaction.setBankReference(bankReference);
        transaction.setPayeeName(payeeName);
        transaction.setDescription(description);
        transaction.setRelatedInvoiceId(relatedInvoiceId);
        transaction.setRelatedReceiptId(relatedReceiptId);
        transaction.setRelatedTransactionId(relatedTransactionId);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCreatedBy(createdBy);
        return transactionRepository.save(transaction);
    }

    private String generateTransactionNo() {
        String prefix = "TRUST-" + LocalDate.now().getYear() + "-";
        long count = transactionRepository.count() + 1;
        String transactionNo = prefix + String.format("%06d", count);

        while (transactionRepository.existsByTransactionNo(transactionNo)) {
            count++;
            transactionNo = prefix + String.format("%06d", count);
        }

        return transactionNo;
    }

    private void validatePositiveAmount(Long amountCents) {
        if (amountCents == null || amountCents <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }
    }

    private void ensureSufficientFunds(CaseTrustLedgerEntity ledger, Long amountCents) {
        if (ledger.getAvailableBalanceCents() < amountCents) {
            throw new RuntimeException("Insufficient trust balance");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
