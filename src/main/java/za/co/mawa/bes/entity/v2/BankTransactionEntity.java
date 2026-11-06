package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "bank_transaction") @Getter @Setter
public class BankTransactionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "source_import_id", nullable = false) private Long sourceImportId;
    @Column(nullable = false, length = 64) private String fingerprint;
    @Column(name = "account_number", nullable = false, length = 40) private String accountNumber;
    @Column(name = "transaction_date", nullable = false) private LocalDate transactionDate;
    @Column(name = "amount_cents", nullable = false) private Long amountCents;
    @Column(name = "service_fee_cents", nullable = false) private Long serviceFeeCents = 0L;
    @Column(name = "reference_text", nullable = false, length = 500) private String referenceText;
    @Column(name = "cheque_number", length = 80) private String chequeNumber;
    @Column(name = "balance_cents") private Long balanceCents;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
