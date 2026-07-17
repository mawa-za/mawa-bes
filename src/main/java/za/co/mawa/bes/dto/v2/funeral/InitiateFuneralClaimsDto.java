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
     * Live EXTERNAL_TENANT values are formatted as EXTERNAL:{tenantId}:{membershipId}:{deceasedPartnerId}:{deceasedType}. Legacy external-cover snapshots remain supported as EXTERNAL:{externalCoverId}.
     */
    private List<String> memberships;

    /**
     * Backwards compatibility with older Flutter builds.
     */
    private List<String> membershipIds;
    private List<String> sourceReferences;

    /**
     * Preferred claim amount mode from Flutter. If omitted, backend derives it from selected count.
     */
    private String claimType;

    private String causeOfDeath;
    private String deathCertificateNo;
    private String notes;
    private String groceryCoverSelectionId;

    public List<String> getMemberships() {
        Set<String> merged = new LinkedHashSet<>();
        addAllNonBlank(merged, memberships);
        addAllNonBlank(merged, membershipIds);
        addAllNonBlank(merged, sourceReferences);

        Set<String> stableSelections = new LinkedHashSet<>();
        merged.stream()
                .filter(this::isStableSelectionId)
                .forEach(stableSelections::add);
        return new ArrayList<>(stableSelections.isEmpty() ? merged : stableSelections);
    }

    public List<String> getMembershipIds() {
        return membershipIds;
    }

    public List<String> getSourceReferences() {
        return sourceReferences;
    }

    public String getClaimType() {
        return claimType;
    }

    public String getEffectiveClaimType(int selectedCoverCount) {
        String value = claimType == null ? "" : claimType.trim().toUpperCase();
        if ("COMBINATION".equals(value)) return "COMBINATION";
        if ("FUNERAL".equals(value)) return "FUNERAL";
        return selectedCoverCount > 1 ? "COMBINATION" : "FUNERAL";
    }

    public String getCauseOfDeath() {
        return causeOfDeath;
    }

    public String getDeathCertificateNo() {
        return deathCertificateNo;
    }

    public String getNotes() { return notes; }
    public String getGroceryCoverSelectionId() { return groceryCoverSelectionId; }

    private boolean isStableSelectionId(String value) {
        return value.startsWith("LOCAL:") || value.startsWith("EXTERNAL:");
    }

    private void addAllNonBlank(Set<String> target, List<String> values) {
        if (values == null) return;
        values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .forEach(target::add);
    }
}
