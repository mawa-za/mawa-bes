package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "card_terminal")
public class CardTerminalEntity {
    @Id @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 36) private String id;
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "merchant_number", length = 100) private String merchantNumber;
    @Column(name = "location_code", length = 100) private String locationCode;
    @Column(nullable = false) private Boolean active = true;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "created_by", length = 255) private String createdBy;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "updated_by", length = 255) private String updatedBy;
    @PrePersist void create(){ createdAt=LocalDateTime.now(); updatedAt=createdAt; if(active==null)active=true; }
    @PreUpdate void update(){ updatedAt=LocalDateTime.now(); }
}
