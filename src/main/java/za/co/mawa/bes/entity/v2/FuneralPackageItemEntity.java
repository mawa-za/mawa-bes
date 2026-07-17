package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @Builder
@Entity @Table(name = "funeral_package_item",
        uniqueConstraints = @UniqueConstraint(name="uq_funeral_package_product", columnNames={"funeral_package_id","product_id"}))
public class FuneralPackageItemEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name="funeral_package_id", nullable=false) private String funeralPackageId;
    @Column(name="product_id", nullable=false) private String productId;
    @Column(name="product_code", nullable=false) private String productCode;
    @Column(name="product_description", nullable=false) private String productDescription;
    @Column(name="quantity", nullable=false) private Integer quantity;
    @Column(name="unit_price_cents", nullable=false) private Long unitPriceCents;
    @Column(name="line_total_cents", nullable=false) private Long lineTotalCents;
}
