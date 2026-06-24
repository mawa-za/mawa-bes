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
@Builder
public class PartnerAttributePKEntity implements Serializable {
    @Column(name = "partner")
    private String partner;
    @Column(name = "attribute")
    private String attribute;
}
