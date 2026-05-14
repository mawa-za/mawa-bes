package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "number_sequence",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_number_sequence_type", columnNames = "seq_type")
        }
)
public class NumberSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seq_type", nullable = false, length = 64)
    private String seqType;

    @Column(name = "next_no", nullable = false)
    private Long nextNo;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getSeqType() {
        return seqType;
    }

    public void setSeqType(String seqType) {
        this.seqType = seqType;
    }

    public Long getNextNo() {
        return nextNo;
    }

    public void setNextNo(Long nextNo) {
        this.nextNo = nextNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
