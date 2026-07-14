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
public class PayAppMasterDataSnapshotResponse {
    private List<PayAppPartnerSyncDto> partners;
    private List<PayAppMemberSyncDto> memberships;
    private List<PayAppPlanSyncDto> plans;
    private List<PayAppFieldOptionSyncDto> fieldOptions;
    private String nextCursor;
    private boolean hasMore;
    private long snapshotWatermark;
    private long totalPartners;
    private long totalMemberships;
}
