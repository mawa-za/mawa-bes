package za.co.mawa.bes.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.controller.ClaimController;
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;
import za.co.mawa.bes.service.v2.BackgroundExecutionContextService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InternalScheduledJobService {
    private final BackgroundExecutionContextService backgroundExecutionContextService;
    private final TransactionService transactionService;
    private final MembershipService membershipService;
    private final PaymentRequestService paymentRequestService;
    private final ClaimController claimController;
    private final za.co.mawa.bes.service.v2.MembershipChangeService membershipChangeService;
    private final za.co.mawa.bes.service.v2.PremiumGenerationService premiumGenerationService;
    private final za.co.mawa.bes.service.v2.MembershipLapseService membershipLapseService;

    public InternalScheduledJobService(
            BackgroundExecutionContextService backgroundExecutionContextService,
            TransactionService transactionService,
            MembershipService membershipService,
            PaymentRequestService paymentRequestService,
            ClaimController claimController,
            za.co.mawa.bes.service.v2.MembershipChangeService membershipChangeService,
            za.co.mawa.bes.service.v2.PremiumGenerationService premiumGenerationService,
            za.co.mawa.bes.service.v2.MembershipLapseService membershipLapseService
    ) {
        this.backgroundExecutionContextService = backgroundExecutionContextService;
        this.transactionService = transactionService;
        this.membershipService = membershipService;
        this.paymentRequestService = paymentRequestService;
        this.claimController = claimController;
        this.membershipChangeService = membershipChangeService;
        this.premiumGenerationService = premiumGenerationService;
        this.membershipLapseService = membershipLapseService;
    }

    public Map<String, Object> run(String jobCode) {
        String normalizedJobCode = jobCode == null ? "" : jobCode.trim().toUpperCase();
        backgroundExecutionContextService.establish();
        try {
            Map<String, Object> result = switch (normalizedJobCode) {
                case "CLAIM_PAYMENT_REQUESTS" -> processApprovedClaims(true);
                case "MEMBERSHIP_STATUS_UPDATE" -> membershipStatusUpdate();
                case "MEMBERSHIP_LAPSE" -> membershipLapse();
                case "COMPLETE_PAYMENT_REQUESTS" -> completeApprovedPaymentRequests();
                case "CLAIM_PROCESSING" -> processApprovedClaims(false);
                case "PREMIUM_GENERATION" -> new LinkedHashMap<>(
                        premiumGenerationService.runConfiguredAutomaticGeneration(BackgroundExecutionContextService.BACKGROUND_USERNAME)
                );
                default -> throw new IllegalArgumentException("Unknown scheduled job: " + jobCode);
            };
            result.put("jobCode", normalizedJobCode);
            Object failed = result.get("failed");
            result.put("success", !(failed instanceof Number) || ((Number) failed).intValue() == 0);
            return result;
        } finally {
            backgroundExecutionContextService.clear();
        }
    }

    private Map<String, Object> processApprovedClaims(boolean generatePaymentRequest) {
        Set<String> claimIds = new LinkedHashSet<>();
        TransactionViewDto query = new TransactionViewDto();
        query.setType(TransactionType.CLAIM);
        query.setStatus(String.valueOf(Status.APPROVED));
        for (TransactionViewEntity claim : transactionService.searchV2(query)) {
            if (claim != null && claim.getTransactionId() != null) {
                claimIds.add(claim.getTransactionId());
            }
        }

        int completed = 0;
        List<String> failures = new ArrayList<>();
        for (String claimId : claimIds) {
            try {
                if (generatePaymentRequest) {
                    requireSuccess(
                            claimController.generatePaymentRequests(claimId),
                            "generate payment request",
                            claimId
                    );
                }
                requireSuccess(
                        claimController.complete(
                                claimId,
                                "Processed by scheduled job",
                                generatePaymentRequest
                                        ? "Payment request generated and claim processed"
                                        : "Claim processed"
                        ),
                        "process claim",
                        claimId
                );
                completed++;
            } catch (Exception ex) {
                failures.add(claimId + ": " + safeMessage(ex));
            }
        }
        return result(claimIds.size(), completed, failures);
    }

    private Map<String, Object> membershipStatusUpdate() {
        int appliedPlanChanges = membershipChangeService.applyDuePlanChanges(java.time.LocalDate.now(), SYSTEM_USER);
        String message = membershipService.scheduledStatusChange();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempted", 1);
        result.put("completed", "Scheduling Error Occurred".equals(message) ? 0 : 1);
        result.put("failed", "Scheduling Error Occurred".equals(message) ? 1 : 0);
        result.put("message", message);
        result.put("appliedMembershipPlanChanges", appliedPlanChanges);
        return result;
    }

    private Map<String, Object> membershipLapse() {
        za.co.mawa.bes.dto.v2.membership.lapse.MembershipLapseRunResultDto lapseResult =
                membershipLapseService.runConfiguredAutomaticLapse(SYSTEM_USER);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempted", lapseResult.getEvaluatedMemberships());
        result.put("completed", lapseResult.getLapsedMemberships());
        result.put("failed", 0);
        result.put("skipped", lapseResult.isSkipped());
        result.put("reason", lapseResult.getReason());
        result.put("threshold", lapseResult.getThreshold());
        result.put("membershipsWithOverduePremiums", lapseResult.getMembershipsWithOverduePremiums());
        result.put("lapsedMembershipIds", lapseResult.getLapsedMembershipIds());
        result.put("runDate", lapseResult.getRunDate());
        return result;
    }

    private Map<String, Object> completeApprovedPaymentRequests() {
        PaymentRequestQueryDto query = new PaymentRequestQueryDto();
        query.setStatus(String.valueOf(Status.APPROVED));
        Set<String> ids = new LinkedHashSet<>();
        for (PaymentRequestQueryDto paymentRequest : paymentRequestService.getAllUsingView(query)) {
            if (paymentRequest != null && paymentRequest.getId() != null) {
                ids.add(paymentRequest.getId());
            }
        }

        int completed = 0;
        List<String> failures = new ArrayList<>();
        for (String id : ids) {
            try {
                paymentRequestService.complete(id);
                completed++;
            } catch (Exception ex) {
                failures.add(id + ": " + safeMessage(ex));
            }
        }
        return result(ids.size(), completed, failures);
    }

    private void requireSuccess(ResponseEntity<?> response, String action, String id) {
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            String body = response == null ? "no response" : String.valueOf(response.getBody());
            throw new IllegalStateException(
                    "Unable to " + action + " for " + id + ": " + body
            );
        }
    }

    private Map<String, Object> result(int attempted, int completed, List<String> failures) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempted", attempted);
        result.put("completed", completed);
        result.put("failed", failures.size());
        result.put("failures", failures);
        return result;
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }
}
