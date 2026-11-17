package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.MembershipPremiumBulkEditRequest;
import za.co.mawa.bes.dto.v2.MembershipPremiumBulkEditResponse;
import za.co.mawa.bes.dto.v2.MembershipPremiumEditRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipPremiumBulkEditService {
    public static final int MAX_LINES = 100;
    private final MembershipPremiumEditService editService;

    /** A failed line rolls back all created edit and approval requests in the batch. */
    @Transactional
    public MembershipPremiumBulkEditResponse submit(MembershipPremiumBulkEditRequest request, String actor) {
        if (request == null || request.getLines() == null || request.getLines().isEmpty()) {
            throw new IllegalArgumentException("Select at least one premium period");
        }
        List<MembershipPremiumBulkEditRequest.Line> lines = request.getLines();
        if (lines.size() > MAX_LINES) {
            throw new IllegalArgumentException("A batch can contain at most " + MAX_LINES + " premium periods");
        }
        if (request.getReason() == null || request.getReason().isBlank()
                || request.getReason().trim().length() > 1000) {
            throw new IllegalArgumentException("Enter a reason of up to 1000 characters");
        }
        Set<String> seenPremiums = new HashSet<>();
        for (MembershipPremiumBulkEditRequest.Line line : lines) {
            if (line == null || line.getMembershipId() == null || line.getMembershipId().isBlank()
                    || line.getPremiumId() == null || line.getPremiumId().isBlank()) {
                throw new IllegalArgumentException("Every row must specify a membership and premium period");
            }
            if (!seenPremiums.add(line.getPremiumId())) {
                throw new IllegalArgumentException("The same premium period was selected more than once");
            }
            if (line.getAmountCents() == null || line.getAmountCents() <= 0L
                    || line.getExpectedAmountCents() == null || line.getExpectedAmountCents() < 0L
                    || line.getExpectedPaidAmountCents() == null || line.getExpectedPaidAmountCents() < 0L) {
                throw new IllegalArgumentException("Each amount must be positive and have its reviewed original amount");
            }
        }

        String batchId = UUID.randomUUID().toString();
        List<MembershipPremiumBulkEditResponse.ApprovalLine> approvals = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            var edit = new MembershipPremiumEditRequest();
            edit.setAmountCents(line.getAmountCents());
            edit.setExpectedAmountCents(line.getExpectedAmountCents());
            edit.setExpectedPaidAmountCents(line.getExpectedPaidAmountCents());
            edit.setReason(request.getReason().trim());
            ApprovalRequestResponse approval = editService.requestBatchEdit(
                    line.getMembershipId().trim(), line.getPremiumId().trim(), edit,
                    actor, batchId, i + 1, lines.size());
            approvals.add(new MembershipPremiumBulkEditResponse.ApprovalLine(
                    line.getMembershipId(), line.getPremiumId(), approval.getId()));
        }
        return new MembershipPremiumBulkEditResponse(batchId, approvals.size(), List.copyOf(approvals));
    }
}
