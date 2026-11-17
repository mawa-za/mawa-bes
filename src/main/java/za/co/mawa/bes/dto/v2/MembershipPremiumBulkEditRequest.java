package za.co.mawa.bes.dto.v2;

import lombok.Data;
import java.util.List;

@Data
public class MembershipPremiumBulkEditRequest {
    private String reason;
    private List<Line> lines;

    @Data
    public static class Line {
        private String membershipId;
        private String premiumId;
        private Long amountCents;
        private Long expectedAmountCents;
        private Long expectedPaidAmountCents;
    }
}
