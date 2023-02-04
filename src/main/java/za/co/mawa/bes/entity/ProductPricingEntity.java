package za.co.mawa.bes.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_pricing")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class ProductPricingEntity {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ProductPricingPKEntity productPricingPKEntity;
    private BigDecimal value;
}
