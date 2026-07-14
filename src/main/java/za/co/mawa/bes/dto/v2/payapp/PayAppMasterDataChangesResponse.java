package za.co.mawa.bes.dto.v2.payapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayAppMasterDataChangesResponse {
    private List<PayAppPartnerSyncDto> partnerUpserts;
    private List<PayAppMemberSyncDto> membershipUpserts;
    private List<PayAppPlanSyncDto> planUpserts;
    private List<PayAppFieldOptionSyncDto> fieldOptionUpserts;
    private List<String> deletedPartnerIds;
    private List<String> deletedMembershipIds;
    private List<String> deletedPlanIds;
    private List<PayAppFieldOptionKeyDto> deletedFieldOptions;
    private String nextCursor;
    private boolean hasMore;
    private boolean resetRequired;
    private long afterWatermark;
    private long nextWatermark;
    private long totalPartners;
    private long totalMemberships;
}
