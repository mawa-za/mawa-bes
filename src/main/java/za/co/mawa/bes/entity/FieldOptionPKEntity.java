package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@EqualsAndHashCode
@Builder
public class FieldOptionPKEntity implements Serializable {
    @Column(name = "field")
    private String field;
    @Column(name = "code")
    private String code;
    @Column(name = "type")
    private String type;
}
