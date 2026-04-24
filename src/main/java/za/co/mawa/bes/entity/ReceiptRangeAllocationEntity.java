package za.co.mawa.bes.entity;


import jakarta.persistence.*;
        import java.time.Instant;

@Entity
@Table(
        name = "receipt_range_allocation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_active",
                columnNames = {"device_id", "status"}
        )
)
public class ReceiptRangeAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "from_no", nullable = false)
    private long fromNo;

    @Column(name = "to_no", nullable = false)
    private long toNo;

    @Column(name = "next_no", nullable = false)
    private long nextNo;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt = Instant.now();

    protected ReceiptRangeAllocationEntity() {}

    public ReceiptRangeAllocationEntity(String deviceId, long fromNo, long toNo) {
        this.deviceId = deviceId;
        this.fromNo = fromNo;
        this.toNo = toNo;
        this.nextNo = fromNo;
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public long getFromNo() { return fromNo; }
    public long getToNo() { return toNo; }
    public long getNextNo() { return nextNo; }
    public String getStatus() { return status; }

    public void setNextNo(long nextNo) { this.nextNo = nextNo; }
    public void setStatus(String status) { this.status = status; }
}