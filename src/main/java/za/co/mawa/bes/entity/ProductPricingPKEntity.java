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
public class ProductPricingPKEntity implements Serializable {
    @Column(name = "product")
    private String product;
    @Column(name = "pricing")
    private String pricing;


}
