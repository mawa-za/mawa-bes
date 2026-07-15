package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "funeral_tenant_integration_config")
public class FuneralTenantIntegrationConfigEntity {
    @Id
    @Column(length = 255)
    private String id;

    @Column(name = "membership_source_mode", nullable = false, length = 40)
    private String membershipSourceMode;

    @Column(name = "external_tenant_id", length = 128)
    private String externalTenantId;

    @Column(name = "external_tenant_name")
    private String externalTenantName;

    @Column(name = "external_tenant_partner_id")
    private String externalTenantPartnerId;

    @Column(name = "membership_lookup_enabled", nullable = false)
    private Boolean membershipLookupEnabled;

    @Column(name = "claim_creation_enabled", nullable = false)
    private Boolean claimCreationEnabled;

    @Column(name = "claim_status_sync_enabled", nullable = false)
    private Boolean claimStatusSyncEnabled;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) id = "DEFAULT";
        if (membershipSourceMode == null || membershipSourceMode.isBlank()) membershipSourceMode = "LOCAL_ONLY";
        if (membershipLookupEnabled == null) membershipLookupEnabled = true;
        if (claimCreationEnabled == null) claimCreationEnabled = true;
        if (claimStatusSyncEnabled == null) claimStatusSyncEnabled = true;
        if (active == null) active = true;
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
