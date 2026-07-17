package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "manual_receipt_cutover_configuration")
public class ManualReceiptCutoverConfigurationEntity {
    @Id
    @Column(name = "id", length = 36)
    private String id;
    @Column(name = "mawapay_go_live_date", nullable = false)
    private LocalDate mawaPayGoLiveDate;
    @Column(name = "legacy_capture_close_date")
    private LocalDate legacyCaptureCloseDate;
    @Column(name = "emergency_receipt_requires_proof", nullable = false)
    private Boolean emergencyReceiptRequiresProof = true;
    @Column(name = "legacy_capture_enabled", nullable = false)
    private Boolean legacyCaptureEnabled = true;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}
