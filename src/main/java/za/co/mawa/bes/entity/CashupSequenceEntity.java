package za.co.mawa.bes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cashup_sequence")
public class CashupSequenceEntity {

    @Id
    private Integer id; // always 1

    @Column(name = "next_no", nullable = false)
    private long nextNo;

    protected CashupSequenceEntity() {}

    public CashupSequenceEntity(Integer id, long nextNo) {
        this.id = id;
        this.nextNo = nextNo;
    }

    public Integer getId() { return id; }
    public long getNextNo() { return nextNo; }
    public void setNextNo(long nextNo) { this.nextNo = nextNo; }
}