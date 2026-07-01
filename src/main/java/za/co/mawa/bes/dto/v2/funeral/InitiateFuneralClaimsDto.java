package za.co.mawa.bes.dto.v2.funeral;

import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Setter
public class InitiateFuneralClaimsDto {
    /**
     * Stable selection ids returned by GET /v2/funeral/check-membership/{identityNumber}.
     * LOCAL_TENANT values are formatted as LOCAL:{membershipId}:{deceasedPartnerId}:{deceasedType}.
     * EXTERNAL_TENANT values are formatted as EXTERNAL:{externalCoverId}.
     */
    private List<String> memberships;

    /**
     * Backwards compatibility with older Flutter builds.
     */
    private List<String> membershipIds;
    private List<String> sourceReferences;

    private String causeOfDeath;
    private String deathCertificateNo;
    private String notes;

    public List<String> getMemberships() {
        Set<String> merged = new LinkedHashSet<>();
        addAllNonBlank(merged, memberships);
        addAllNonBlank(merged, membershipIds);
        addAllNonBlank(merged, sourceReferences);
        return new ArrayList<>(merged);
    }

    public List<String> getMembershipIds() {
        return membershipIds;
    }

    public List<String> getSourceReferences() {
        return sourceReferences;
    }

    public String getCauseOfDeath() {
        return causeOfDeath;
    }

    public String getDeathCertificateNo() {
        return deathCertificateNo;
    }

    public String getNotes() {
        return notes;
    }

    private void addAllNonBlank(Set<String> target, List<String> values) {
        if (values == null) return;
        values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .forEach(target::add);
    }
}
