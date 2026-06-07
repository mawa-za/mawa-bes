package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "case_trust_ledger")
public class CaseTrustLedgerEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "case_id", nullable = false, unique = true)
    private String caseId;

    @Column(name = "client_partner_id", nullable = false)
    private String clientPartnerId;

    @Column(nullable = false)
    private String currency = "ZAR";

    @Column(name = "total_received_cents", nullable = false)
    private Long totalReceivedCents = 0L;

    @Column(name = "total_transferred_cents", nullable = false)
    private Long totalTransferredCents = 0L;

    @Column(name = "total_refunded_cents", nullable = false)
    private Long totalRefundedCents = 0L;

    @Column(name = "total_paid_out_cents", nullable = false)
    private Long totalPaidOutCents = 0L;

    @Column(name = "available_balance_cents", nullable = false)
    private Long availableBalanceCents = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
