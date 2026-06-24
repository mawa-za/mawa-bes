package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "product_pricing")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class ProductPricingEntity {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ProductPricingPKEntity productPricingPKEntity;
    private BigDecimal value;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
}
