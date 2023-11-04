package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "product_attribute")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class ProductAttributeEntity implements Serializable {
    @EmbeddedId
    ProductAttributePKEntity productAttributePKEntity;
    @Column(name = "value")
    private String value;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
}
