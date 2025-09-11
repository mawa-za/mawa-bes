package za.co.mawa.bes.entity;


import jakarta.persistence.*;
        import lombok.*;
        import java.math.BigDecimal;

@Entity
@Table(name = "product_storage_bin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStorageBinEntity {

    @EmbeddedId
    private ProductStorageBinPKEntity id;

    @Column(name = "min_qty", precision = 10, scale = 2)
    private BigDecimal minQty = BigDecimal.ZERO;

    @Column(name = "max_qty", precision = 10, scale = 2)
    private BigDecimal maxQty = BigDecimal.ZERO;
}
