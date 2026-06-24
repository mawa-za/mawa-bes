package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "receipt_sequence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptSequenceEntity {

    @Id
    private Integer id; // always 1

    @Column(name = "next_no", nullable = false)
    private long nextNo;

}