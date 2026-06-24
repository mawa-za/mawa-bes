package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;

@Entity
@Table(name = "field")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class FieldEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(unique = true, name = "code")
    private String code;
    @Column(name = "description")
    private String description;
    @Column(name = "valid_from")
    private String validFrom;
    @Column(name = "valid_to")
    private String validTo;
}
