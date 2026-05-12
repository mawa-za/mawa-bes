package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "cashup_receipt")
public class CashupReceiptEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "id", length = 255)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashup_id", nullable = false)
    private CashupEntity cashup;

    @Column(name = "receipt_id", length = 255)
    private String receiptId;

    @Column(name = "receipt_no")
    private Long receiptNo;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents = 0L;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
