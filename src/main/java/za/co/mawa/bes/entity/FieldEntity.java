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
public class FieldEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(unique = true, name = "code")
    private String code;
    @Column(name = "description")
    private String description;
    @Column(name = "valid_from")
    private String validFrom;
    @Column(name = "valid_to")
    private String validTo;
}
