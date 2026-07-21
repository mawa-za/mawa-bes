package za.co.mawa.bes.service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.controller.ClaimController;
import za.co.mawa.bes.dto.payment.request.PaymentRequestQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.entity.UserEntity;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InternalScheduledJobService {
    private static final String SYSTEM_USER = "system";

    private final UserService userService;
    private final TransactionService transactionService;
    private final MembershipService membershipService;
    private final PaymentRequestService paymentRequestService;
    private final ClaimController claimController;
    private final za.co.mawa.bes.service.v2.MembershipChangeService membershipChangeService;

    public InternalScheduledJobService(
            UserService userService,
            TransactionService transactionService,
            MembershipService membershipService,
            PaymentRequestService paymentRequestService,
            ClaimController claimController,
            za.co.mawa.bes.service.v2.MembershipChangeService membershipChangeService
    ) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.membershipService = membershipService;
        this.paymentRequestService = paymentRequestService;
        this.claimController = claimController;
        this.membershipChangeService = membershipChangeService;
    }

    public Map<String, Object> run(String jobCode) {
        String normalizedJobCode = jobCode == null ? "" : jobCode.trim().toUpperCase();
        establishSystemExecutionContext();
        try {
            Map<String, Object> result = switch (normalizedJobCode) {
                case "CLAIM_PAYMENT_REQUESTS" -> processApprovedClaims(true);
                case "MEMBERSHIP_STATUS_UPDATE" -> membershipStatusUpdate();
                case "MEMBERSHIP_LAPSE" -> membershipLapse();
                case "COMPLETE_PAYMENT_REQUESTS" -> completeApprovedPaymentRequests();
                case "CLAIM_PROCESSING" -> processApprovedClaims(false);
                default -> throw new IllegalArgumentException("Unknown scheduled job: " + jobCode);
            };
            result.put("jobCode", normalizedJobCode);
            Object failed = result.get("failed");
            result.put("success", !(failed instanceof Number) || ((Number) failed).intValue() == 0);
            return result;
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
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
        TransactionViewDto query = new TransactionViewDto();
        query.setType(TransactionType.MEMBERSHIP);
        List<TransactionViewEntity> memberships = transactionService.searchV2(query);
        try {
            String message = membershipService.handleMembershipLapse(memberships);
            Map<String, Object> result = result(memberships.size(), memberships.size(), List.of());
            result.put("message", message);
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("Membership lapse processing failed", ex);
        }
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

    private void establishSystemExecutionContext() {
        try {
            UserEntity systemUser = userService.getUserEntityByName(SYSTEM_USER);
            if (systemUser == null) {
                userService.getUserByName(SYSTEM_USER);
                systemUser = userService.getUserEntityByName(SYSTEM_USER);
            }
            if (systemUser == null) {
                throw new IllegalStateException("Tenant system user is not available");
            }

            UserDetails principal = User.withUsername(SYSTEM_USER)
                    .password("")
                    .authorities("SYSTEM")
                    .build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    )
            );
            UserContext.setCurrentUser(SYSTEM_USER);
            UserContext.setCurrentUserId(systemUser.getId());
            UserContext.setCurrentUserPartner(systemUser.getPartner());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to establish trusted system execution context", ex);
        }
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
