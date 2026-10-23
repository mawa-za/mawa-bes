package za.co.mawa.bes.dto.v2.tombstone;

import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Request capture only; operational orders remain in Tombstone Management. */
public final class TombstoneInstallationDtos {
    private TombstoneInstallationDtos() {}

    @Data
    public static class SelectedCover {
        private String tenantId;
        private String membershipId;
        private String deceasedPartnerId;
        private String deceasedType;
        // Display-only snapshot for resumable requests; entitlement is always recalculated in the owning tenant.
        private String membershipNumber;
        private String burialSocietyName;
        private String sourceTenantName;
        private Long tombstoneAmountCents;
    }

    @Data
    public static class Request {
        private String customerPartnerId;
        private String deceasedPartnerId;
        private String deceasedName;
        private String deceasedIdentityNumber;
        private LocalDate dateOfDeath;
        private String causeOfDeath;
        private String deathCertificateNo;
        private String familyRepresentativePartnerId;
        private String familyRepresentativeName;
        private String familyRepresentativeContact;
        private String cemeteryName;
        private String cemeteryArea;
        private String graveNumber;
        private String salesArea;
        private String workcenterId;
        private LocalDate expectedInstallationDate;
        private String notes;
        private Long discountCents;
        private List<TombstoneDtos.ItemRequest> items = new ArrayList<>();
        private List<SelectedCover> selectedCovers = new ArrayList<>();
    }
}
