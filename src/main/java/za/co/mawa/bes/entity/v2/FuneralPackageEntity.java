package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
@Entity
@Table(name = "funeral_package")
public class FuneralPackageEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    /** FIXED_PRICE keeps one package price; ITEM_TOTAL derives the price from included products. */
    @Column(name = "pricing_mode", nullable = false)
    private String pricingMode = "ITEM_TOTAL";

    @Column(name = "base_price_cents", nullable = false)
    private Long basePriceCents = 0L;

    @Column(name = "inclusions_json", columnDefinition = "json")
    private String inclusionsJson;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Transient
    private List<FuneralPackageItemEntity> products;
}
