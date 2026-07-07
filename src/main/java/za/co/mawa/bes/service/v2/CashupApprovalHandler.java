package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.v2.CashupRepository;

@Component
@RequiredArgsConstructor
public class CashupApprovalHandler implements ApprovalCompletionHandler, ApprovalSubmissionHandler {

    private final CashupRepository cashupRepository;

    @Override
    public ApprovalType supports() {
        return ApprovalType.CASHUP;
    }

    @Override
    public void onSubmit(ApprovalRequestEntity approvalRequest, String actionBy) {
        CashupEntity cashup = cashupRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + approvalRequest.getReferenceId()));
        cashup.setStatus("SUBMITTED");
        cashup.setApprovalRequestId(approvalRequest.getId());
        cashup.setUpdatedBy(actionBy);
        cashupRepository.save(cashup);
    }

    @Override
    public void onApproved(ApprovalRequestEntity approvalRequest, String actionBy) {
        CashupEntity cashup = cashupRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + approvalRequest.getReferenceId()));
        cashup.setStatus("APPROVED");
        cashup.setUpdatedBy(actionBy);
        cashupRepository.save(cashup);
    }
}
