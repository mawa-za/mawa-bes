package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AdditionalMembershipApprovalHandler implements ApprovalCompletionHandler {

    private final MembershipRepository membershipRepository;

    @Override
    public ApprovalType supports() {
        return ApprovalType.ADDITIONAL_MEMBERSHIP;
    }

    @Override
    @Transactional
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        MembershipEntity membership = membershipRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new IllegalStateException(
                        "Additional membership not found: " + approvalRequest.getReferenceId()));
        membership.setStatus("ACTIVE");
        membership.setApprovalRequestId(approvalRequest.getId());
        membership.setUpdatedBy(actionBy);
        membership.setUpdatedAt(LocalDateTime.now());
        membershipRepository.save(membership);
        approvalRequest.setDescription("Additional membership approved and activated.");
    }

    @Override
    @Transactional
    public void onRejected(ApprovalRequestEntity approvalRequest, String actionBy) {
        updateRejected(approvalRequest, actionBy, "REJECTED");
    }

    @Override
    @Transactional
    public void onCancelled(ApprovalRequestEntity approvalRequest, String actionBy) {
        updateRejected(approvalRequest, actionBy, "CANCELLED");
    }

    private void updateRejected(ApprovalRequestEntity approvalRequest, String actionBy, String status) {
        membershipRepository.findById(approvalRequest.getReferenceId()).ifPresent(membership -> {
            membership.setStatus(status);
            membership.setApprovalRequestId(approvalRequest.getId());
            membership.setUpdatedBy(actionBy);
            membership.setUpdatedAt(LocalDateTime.now());
            membershipRepository.save(membership);
        });
    }
}
