package za.co.mawa.bes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "cashup_sequence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashupSequenceEntity {

    @Id
    private Integer id; // always 1

    @Column(name = "next_no", nullable = false)
    private long nextNo;

}