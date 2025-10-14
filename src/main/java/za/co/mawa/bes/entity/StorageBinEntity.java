package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "storage_bin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageBinEntity {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String binId;

    @Column(name = "warehouse_id", length = 60, nullable = false)
    private String warehouseId;

    @Column(name = "description")
    private String description;

    @Column(name = "aisle", length = 10, nullable = false)
    private String aisle;

    @Column(name = "rack", length = 10, nullable = false)
    private String stack;

    @Column(name = "shelf", length = 10, nullable = false)
    private String shelf;

    @Column(name = "bin_code", length = 50, nullable = false, unique = true)
    private String binCode;

    @Column(name = "bin_type", length = 20)
    private String binType;

    @Column(name = "capacity", precision = 10, scale = 2)
    private BigDecimal capacity;

    @Column(name = "unit_of_measure", length = 10)
    private String unitOfMeasure = "KG";

    @Column(name = "status", length = 20)
    private String status = "AVAILABLE";

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published")
    private Boolean published = false;

    @Column(name = "product_id")
    private String productId;

}
