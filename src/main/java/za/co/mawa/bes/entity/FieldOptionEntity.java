package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "field_option")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class FieldOptionEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected FieldOptionPKEntity fieldOptionPKEntity;
    @Column(name = "description")
    private String description;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

}
