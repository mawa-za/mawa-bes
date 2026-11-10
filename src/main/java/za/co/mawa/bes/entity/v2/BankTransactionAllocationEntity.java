package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "bank_transaction_allocation") @Getter @Setter
public class BankTransactionAllocationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "bank_transaction_id", nullable = false) private Long bankTransactionId;
    @Column(name = "allocation_type", nullable = false, length = 40) private String allocationType;
    @Column(name = "object_id", nullable = false) private String objectId;
    @Column(name = "amount_cents", nullable = false) private Long amountCents;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "released_at") private LocalDateTime releasedAt;
    @Column(name = "released_by") private String releasedBy;
}
