package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.v2.ApprovalRequestEntity;
import za.co.mawa.bes.entity.v2.CashupEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.repository.v2.CashupRepository;
import za.co.mawa.bes.repository.v2.CashupDepositRepository;

@Component
@RequiredArgsConstructor
public class CashupApprovalHandler implements ApprovalCompletionHandler, ApprovalSubmissionHandler {

    private final CashupRepository cashupRepository;
    private final CashupDepositRepository deposits;

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
        String source = cashup.getSource() == null ? "" : cashup.getSource().trim().toUpperCase();
        if (!"MAWA_PAY_EFT".equals(source) && !"ERP_ONLINE_EFT".equals(source)) {
            var linked = deposits.findByCashupIdOrderByDepositDateDescCreatedAtDesc(cashup.getId());
            if (linked.isEmpty() || linked.stream().anyMatch(d -> d.getBankDepositId() == null)) {
                throw new IllegalStateException("Cashup approval requires verified Deposit IDs for all bank deposits");
            }
        }
        cashup.setStatus("APPROVED");
        cashup.setUpdatedBy(actionBy);
        cashupRepository.save(cashup);
    }

    @Override
    public void onRejected(ApprovalRequestEntity approvalRequest, String actionBy, String reason) {
        CashupEntity cashup = cashupRepository.findById(approvalRequest.getReferenceId())
                .orElseThrow(() -> new IllegalArgumentException("Cashup not found: " + approvalRequest.getReferenceId()));
        cashup.setStatus("AWAITING_DEPOSITS");
        cashup.setApprovalRequestId(null);
        cashup.setNotes(reason == null || reason.isBlank() ? "Cashup approval rejected" : reason.trim());
        cashup.setUpdatedBy(actionBy);
        cashupRepository.save(cashup);
    }
}
