package za.co.mawa.bes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "number_sequence")
public class NumberSequenceEntity {

    @Id
    private Integer id; // always 1

    @Column(name = "seq_type", nullable = false, unique = true)
    private String type;

    @Column(name = "next_no", nullable = false)
    private long nextNo;

    protected NumberSequenceEntity() {}

    public NumberSequenceEntity(Integer id,String type, long nextNo) {
        this.id = id;
        this.type =  type;
        this.nextNo = nextNo;
    }

    public Integer getId() { return id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getNextNo() { return nextNo; }
    public void setNextNo(long nextNo) { this.nextNo = nextNo; }
}