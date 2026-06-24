package za.co.mawa.bes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStorageBinPKEntity implements Serializable {
    @Column(name = "bin_id")
    private String binId;

    @Column(name = "product_id")
    private String productId;
}
