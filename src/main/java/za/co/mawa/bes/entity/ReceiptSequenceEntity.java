package za.co.mawa.bes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "receipt_sequence")
public class ReceiptSequenceEntity {

    @Id
    private Integer id; // always 1

    @Column(name = "next_no", nullable = false)
    private long nextNo;

    protected ReceiptSequenceEntity() {}

    public ReceiptSequenceEntity(Integer id, long nextNo) {
        this.id = id;
        this.nextNo = nextNo;
    }

    public Integer getId() { return id; }
    public long getNextNo() { return nextNo; }
    public void setNextNo(long nextNo) { this.nextNo = nextNo; }
}