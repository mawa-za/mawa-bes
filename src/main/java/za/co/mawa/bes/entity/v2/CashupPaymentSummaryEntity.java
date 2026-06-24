package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cashup_payment_summary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_cashup_payment_method",
                        columnNames = {"cashup_id", "payment_method"}
                )
        }
)
public class CashupPaymentSummaryEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "id", length = 255)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashup_id", nullable = false)
    private CashupEntity cashup;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents = 0L;

    @Column(name = "payment_count", nullable = false)
    private Integer paymentCount = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}