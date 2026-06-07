package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "funeral_package")
public class FuneralPackageEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "base_price_cents", nullable = false)
    private Long basePriceCents = 0L;

    @Lob
    @Column(name = "inclusions_json")
    private String inclusionsJson;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
