package za.co.mawa.bes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class ProductAttributePKEntity implements Serializable {
    @Column(name = "product")
    private String product;
    @Column(name = "attribute")
    private String attribute;
}
