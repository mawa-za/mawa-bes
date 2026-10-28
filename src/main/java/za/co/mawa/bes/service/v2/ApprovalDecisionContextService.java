package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimResponse;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestResponse;

/** Builds a stable, readable snapshot at submission time, never from mutable state at decision time. */
@Service
@RequiredArgsConstructor
public class ApprovalDecisionContextService {
    private final ObjectMapper mapper;
    private final MembershipClaimService claims;
    private final ObjectProvider<PaymentRequestService> payments;
    private final ApprovalUserLabelService labels;

    public String snapshot(ApprovalSubmitRequest request) {
        try {
            ObjectNode root;
            String raw = request.getPayloadJson();
            if (raw == null || raw.isBlank()) root = mapper.createObjectNode();
            else {
                JsonNode parsed = mapper.readTree(raw);
                root = parsed instanceof ObjectNode object ? object.deepCopy() : mapper.createObjectNode();
                if (!(parsed instanceof ObjectNode)) root.set("originalDetails", parsed);
            }
            ObjectNode metadata = root.putObject("approvalSummary");
            metadata.put("requestType", request.getApprovalType().name().replace('_', ' '));
            metadata.put("referenceNumber", request.getReferenceNo());
            metadata.put("title", request.getTitle());
            metadata.put("reasonAndInstructions", request.getDescription());
            metadata.put("requestedBy", labels.displayName(request.getRequesterId()));

            if (request.getApprovalType().isMembershipClaimApproval()) {
                try {
                    MembershipClaimResponse claim = claims.getById(request.getReferenceId());
                    ObjectNode values = root.putObject("claimDecisionDetails");
                    put(values, "claimNumber", claim.getClaimNo());
                    put(values, "membershipNumber", claim.getMembershipNo());
                    put(values, "memberName", claim.getMemberName());
                    put(values, "memberIdentityNumber", claim.getMemberIdentityNumber());
                    put(values, "deceasedName", claim.getDeceasedName());
                    put(values, "deceasedIdentityNumber", claim.getDeceasedIdentityNumber());
                    put(values, "claimantName", claim.getClaimantName());
                    put(values, "claimType", claim.getClaimType());
                    put(values, "coveragePlan", claim.getCoveragePlanName());
                    put(values, "dateOfDeath", claim.getDateOfDeath());
                    put(values, "burialDate", claim.getBurialDate());
                    put(values, "dateOfClaim", claim.getClaimDate());
                    put(values, "amountCents", claim.getClaimAmountCents());
                    put(values, "combinedClaimAmountCents", claim.getCombinedClaimAmountCents());
                    put(values, "payoutMethod", claim.getPayoutMethod());
                    put(values, "bankAccountHolder", claim.getAccountHolderName());
                    put(values, "bankName", claim.getBankName());
                    put(values, "bankAccountLastFour", lastFour(claim.getAccountNumber()));
                    put(values, "notes", claim.getNotes());
                } catch (RuntimeException missing) {
                    // An external-provider claim may be stored under a different tenant.
                    root.put("decisionDetailsWarning", "Claim details were not available in this tenant at submission. Review supporting documents.");
                }
            }
            if (request.getApprovalType() == za.co.mawa.bes.enums.ApprovalType.PAYMENT_REQUEST
                    || request.getApprovalType() == za.co.mawa.bes.enums.ApprovalType.PAYMENT
                    || request.getApprovalType() == za.co.mawa.bes.enums.ApprovalType.SUPPLIER_INVOICE) {
                try {
                    PaymentRequestResponse payment = payments.getObject().getById(request.getReferenceId());
                    ObjectNode values = root.putObject("paymentDecisionDetails");
                    put(values, "requestNumber", payment.getRequestNo());
                    put(values, "paymentType", payment.getRequestType());
                    put(values, "payee", payment.getPayeeName());
                    put(values, "amount", payment.getAmount());
                    put(values, "currency", payment.getCurrency());
                    put(values, "paymentMethod", payment.getPaymentMethod());
                    put(values, "bankName", payment.getBankName());
                    put(values, "accountHolder", payment.getAccountHolder());
                    put(values, "bankAccountLastFour", lastFour(payment.getAccountNumber()));
                    put(values, "invoiceNumber", payment.getInvoiceNo());
                    put(values, "paymentReason", payment.getPaymentReason());
                    put(values, "requestedPaymentDate", payment.getRequestedPaymentDate());
                    put(values, "notes", payment.getNotes());
                } catch (RuntimeException missing) {
                    // Some PAYMENT references are batches rather than payment requests.
                    root.put("decisionDetailsWarning", "Payment details could not be resolved at submission. Verify the supporting records before deciding.");
                }
            }
            return mapper.writeValueAsString(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalArgumentException("Invalid approval decision data", error);
        }
    }

    private static void put(ObjectNode root, String key, Object value) {
        if (value == null) return;
        if (value instanceof Number number) root.put(key, new java.math.BigDecimal(number.toString()));
        else root.put(key, value.toString());
    }

    private static String lastFour(String number) {
        if (number == null || number.isBlank()) return null;
        String digits = number.trim();
        return digits.substring(Math.max(0, digits.length() - 4));
    }
}
