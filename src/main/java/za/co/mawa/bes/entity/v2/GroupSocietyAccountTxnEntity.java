package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "group_society_account_txn")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSocietyAccountTxnEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "group_society_id", nullable = false)
    private String groupSocietyId;

    @Column(name = "txn_type", nullable = false)
    private String txnType;

    @Column(nullable = false)
    private String direction;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "balance_before_cents", nullable = false)
    private Long balanceBeforeCents;

    @Column(name = "balance_after_cents", nullable = false)
    private Long balanceAfterCents;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "txn_datetime", insertable = false, updatable = false)
    private Date txnDatetime;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "period")
    private String period;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @Column(name = "created_by")
    private String createdBy;

}