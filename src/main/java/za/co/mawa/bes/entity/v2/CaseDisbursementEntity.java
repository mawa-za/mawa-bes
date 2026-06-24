package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.CaseDisbursementType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "case_disbursement")
public class CaseDisbursementEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_type", nullable = false)
    private CaseDisbursementType disbursementType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(nullable = false)
    private Boolean billable = true;

    @Column(nullable = false)
    private Boolean billed = false;

    @Column(name = "invoice_id")
    private String invoiceId;

    @Column(name = "paid_from_trust", nullable = false)
    private Boolean paidFromTrust = false;

    @Column(name = "trust_transaction_id")
    private String trustTransactionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
