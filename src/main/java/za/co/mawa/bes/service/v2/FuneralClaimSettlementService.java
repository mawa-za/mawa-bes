package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestCreateRequest;
import za.co.mawa.bes.dto.v2.payment.PaymentRequestResponse;
import za.co.mawa.bes.entity.v2.FuneralServiceClaimEntity;
import za.co.mawa.bes.enums.PaymentMethod;
import za.co.mawa.bes.enums.PaymentRequestSourceType;
import za.co.mawa.bes.enums.PaymentRequestStatus;
import za.co.mawa.bes.enums.PaymentRequestType;
import za.co.mawa.bes.repository.v2.FuneralServiceClaimRepository;
import za.co.mawa.bes.service.NumberRangeService;
import za.co.mawa.bes.service.SettingService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FuneralClaimSettlementService {
    private static final String FUNERAL_CLAIM_PAY_SETTING = "FUNERAL_CLAIM_PAY";
    private static final String FUNERAL_CLAIM_SUPPLIER_ATTRIBUTE = "SUPPLIER_PARTNER_ID";

    private final JdbcTemplate jdbc;
    private final FuneralServiceClaimRepository links;
    private final PaymentRequestService payments;
    private final MembershipClaimService membershipClaims;
    private final PaymentRequestFnbPaymentQueueService paymentQueue;
    private final SettingService settingService;
    private final NumberRangeService numberRangeService;

    @Transactional
    public PaymentRequestResponse settleApprovedClaim(String claimId, String actor) {
        Map<String, Object> claim = jdbc.queryForMap("""
                SELECT claim_no, claim_type, membership_id, claim_amount_cents,
                       approved_amount_cents, status, funeral_service_id,
                       funeral_provider_tenant_id
                  FROM membership_claim
                 WHERE id = ?
                 FOR UPDATE
                """, claimId);
        String type = Objects.toString(claim.get("claim_type"), "");
        if (!Set.of("FUNERAL", "COMBINATION").contains(type)) return null;
        String status = Objects.toString(claim.get("status"), "");
        if (!Set.of("APPROVED", "PARTIALLY_APPROVED", "PAYMENT_PENDING", "PAYMENT_PROCESSING", "PAID")
                .contains(status)) return null;

        FuneralServiceClaimEntity localLink = links.findByMembershipClaimId(claimId).orElse(null);
        String providerTenant = firstNonBlank(
                localLink == null ? null : localLink.getProviderTenantId(),
                Objects.toString(claim.get("funeral_provider_tenant_id"), TenantContext.getCurrentTenant()));
        String serviceId = firstNonBlank(
                localLink == null ? null : localLink.getFuneralServiceId(),
                Objects.toString(claim.get("funeral_service_id"), null));
        if (serviceId == null) {
            throw new IllegalStateException("Funeral service reference is missing for claim " + claimId);
        }

        String providerLinkTable = qualified(providerTenant, "funeral_service_claim");
        if (localLink == null || localLink.getServicePaymentRequestId() == null) {
            List<String> existingRequestIds = jdbc.query(
                    "SELECT service_payment_request_id FROM " + providerLinkTable
                            + " WHERE membership_claim_id=? AND service_payment_request_id IS NOT NULL LIMIT 1",
                    (rs, rowNum) -> rs.getString(1), claimId);
            if (!existingRequestIds.isEmpty() && existingRequestIds.get(0) != null) {
                try {
                    return payments.getById(existingRequestIds.get(0));
                } catch (RuntimeException ignored) {
                    // Cross-tenant link can outlive a repaired/deleted local request.
                    // Continue and recreate idempotently in the claim-owning tenant.
                }
            }
        } else {
            try {
                return payments.getById(localLink.getServicePaymentRequestId());
            } catch (RuntimeException ignored) {
                // Repair a stale link by recreating below.
            }
        }

        if (Set.of("PAYMENT_PROCESSING", "PAID").contains(status)) {
            throw new IllegalStateException(
                    "Claim " + Objects.toString(claim.get("claim_no"), claimId)
                            + " is already " + status
                            + " but its payment request link is missing. Reconcile the existing payment before creating another request.");
        }

        Map<String, Object> invoice = ensureMembershipCoverageInvoice(
                providerTenant, serviceId, claimId, claim, localLink);
        long amountCents = approvedAmount(claim);
        if (amountCents <= 0) {
            throw new IllegalStateException("Approved funeral claim amount must be greater than zero");
        }

        String supplierPartnerId = resolveConfiguredFuneralClaimSupplierId();
        String paymentInvoiceNo = Objects.toString(invoice.get("invoice_no"), null);
        PaymentRequestCreateRequest request = new PaymentRequestCreateRequest();
        request.setRequestType(PaymentRequestType.FUNERAL_SERVICE_PAYMENT);
        request.setSourceType(PaymentRequestSourceType.MEMBERSHIP_CLAIM);
        request.setSourceId(claimId);
        request.setPayeePartnerId(supplierPartnerId);
        request.setPayeeName(resolvePartnerName(TenantContext.getCurrentTenant(), supplierPartnerId));
        applySupplierBanking(TenantContext.getCurrentTenant(), supplierPartnerId, request);
        request.setAmount(BigDecimal.valueOf(amountCents, 2));
        request.setCurrency("ZAR");
        request.setInvoiceNo(paymentInvoiceNo);
        request.setExternalReference(paymentInvoiceNo);
        request.setPaymentReason("FUNERAL-SERVICE-COVER");
        request.setRequestedPaymentDate(LocalDate.now());
        request.setIdempotencyKey("FUNERAL-SERVICE:" + claimId);
        request.setNotes("Approved " + type + " claim payment to funeral service provider");

        String effectiveActor = effectiveActor(actor);
        PaymentRequestResponse response = approveAndQueue(payments.create(request, effectiveActor), effectiveActor);

        if (localLink != null) {
            localLink.setServiceInvoiceId(Objects.toString(invoice.get("invoice_id"), null));
            localLink.setServicePaymentRequestId(response.getId());
            localLink.setProviderTenantId(providerTenant);
            localLink.setProviderPartnerId(supplierPartnerId);
            links.save(localLink);
        }
        jdbc.update("UPDATE " + providerLinkTable
                        + " SET service_invoice_id=?, service_payment_request_id=?, provider_tenant_id=?, provider_partner_id=?"
                        + " WHERE membership_claim_id=?",
                invoice.get("invoice_id"), response.getId(), providerTenant, supplierPartnerId, claimId);
        jdbc.update("UPDATE " + qualified(providerTenant, "funeral_service_invoice")
                        + " SET payment_request_id=?, provider_tenant_id=?, cover_tenant_id=? WHERE invoice_id=?",
                response.getId(), providerTenant, TenantContext.getCurrentTenant(), invoice.get("invoice_id"));
        return response;
    }


    @Transactional
    public void reconcileApprovedClaimsForFuneralService(String funeralServiceId, String actor) {
        if (funeralServiceId == null || funeralServiceId.isBlank()) return;
        List<Map<String, Object>> approvedClaims = jdbc.queryForList("""
                SELECT id, claim_type
                  FROM membership_claim
                 WHERE funeral_service_id = ?
                   AND status IN ('APPROVED','PAYMENT_PENDING')
                   AND claim_type IN ('FUNERAL','COMBINATION','GROCERY')
                 ORDER BY created_at
                """, funeralServiceId);
        for (Map<String, Object> row : approvedClaims) {
            String claimId = Objects.toString(row.get("id"), null);
            String claimType = Objects.toString(row.get("claim_type"), "");
            if (claimId == null) continue;
            if (Set.of("FUNERAL", "COMBINATION").contains(claimType)) {
                PaymentRequestResponse paymentRequest = settleApprovedClaim(claimId, actor);
                if (paymentRequest != null) {
                    membershipClaims.linkPaymentRequest(paymentRequest, effectiveActor(actor));
                }
            } else if ("GROCERY".equals(claimType)) {
                var claim = membershipClaims.getById(claimId);
                PaymentRequestResponse paymentRequest = payments.createOrReuseApprovedClaimPayout(
                        claim, null, effectiveActor(actor));
                membershipClaims.linkPaymentRequest(paymentRequest, effectiveActor(actor));
            }
        }

        List<String> groupClaims = jdbc.query("""
                SELECT id
                  FROM group_society_funeral_claim
                 WHERE funeral_service_id = ?
                   AND status = 'APPROVED'
                 ORDER BY created_at
                """, (rs, rowNum) -> rs.getString(1), funeralServiceId);
        for (String groupClaimId : groupClaims) {
            settleApprovedGroupSocietyClaim(groupClaimId, actor);
        }
    }

    @Transactional
    public PaymentRequestResponse settleApprovedGroupSocietyClaim(String claimId, String actor) {
        Map<String, Object> claim = jdbc.queryForMap("""
                SELECT c.id, c.claim_no, c.funeral_service_id, c.group_society_id,
                       c.requested_cover_cents, c.approved_cover_cents, c.status,
                       c.payment_request_id, g.partner_id AS society_partner_id,
                       g.group_no,
                       TRIM(CONCAT_WS(' ', NULLIF(p.name1,''), NULLIF(p.name2,''), NULLIF(p.name3,''))) AS society_name
                  FROM group_society_funeral_claim c
                  JOIN group_society g ON g.id = c.group_society_id
                  JOIN partner p ON p.id = g.partner_id
                 WHERE c.id = ?
                 FOR UPDATE
                """, claimId);
        if (!"APPROVED".equalsIgnoreCase(Objects.toString(claim.get("status"), ""))) return null;

        String existingPaymentRequestId = Objects.toString(claim.get("payment_request_id"), null);
        if (existingPaymentRequestId != null && !existingPaymentRequestId.isBlank()) {
            try {
                return payments.getById(existingPaymentRequestId);
            } catch (RuntimeException ignored) {
                // Reconcile a stale reference below using the idempotency key.
            }
        }

        String serviceId = Objects.toString(claim.get("funeral_service_id"), null);
        long amountCents = number(claim.get("approved_cover_cents"));
        if (amountCents <= 0) amountCents = number(claim.get("requested_cover_cents"));
        if (amountCents <= 0) {
            throw new IllegalStateException("Approved group society cover amount must be greater than zero");
        }

        Map<String, Object> invoice = ensureGroupSocietyCoverageInvoice(serviceId, claimId, claim, amountCents);
        String tenant = TenantContext.getCurrentTenant();
        String supplierPartnerId = resolveConfiguredFuneralClaimSupplierId();
        String paymentInvoiceNo = Objects.toString(invoice.get("invoice_no"), null);

        PaymentRequestCreateRequest request = new PaymentRequestCreateRequest();
        request.setRequestType(PaymentRequestType.FUNERAL_SERVICE_PAYMENT);
        request.setSourceType(PaymentRequestSourceType.GROUP_SOCIETY);
        request.setSourceId(claimId);
        request.setPayeePartnerId(supplierPartnerId);
        request.setPayeeName(resolvePartnerName(tenant, supplierPartnerId));
        applySupplierBanking(tenant, supplierPartnerId, request);
        request.setAmount(BigDecimal.valueOf(amountCents, 2));
        request.setCurrency("ZAR");
        request.setInvoiceNo(paymentInvoiceNo);
        request.setExternalReference(paymentInvoiceNo);
        request.setPaymentReason("FUNERAL-SERVICE-COVER");
        request.setRequestedPaymentDate(LocalDate.now());
        request.setIdempotencyKey("GROUP-SOCIETY-FUNERAL-SERVICE:" + claimId);
        request.setNotes("Approved group society funeral cover payment to funeral service provider");

        String effectiveActor = effectiveActor(actor);
        PaymentRequestResponse response = approveAndQueue(payments.create(request, effectiveActor), effectiveActor);
        jdbc.update("UPDATE group_society_funeral_claim SET payment_request_id=? WHERE id=?",
                response.getId(), claimId);
        jdbc.update("UPDATE funeral_service_invoice"
                        + " SET payment_request_id=?, provider_tenant_id=?, cover_tenant_id=? WHERE invoice_id=?",
                response.getId(), tenant, tenant, invoice.get("invoice_id"));
        return response;
    }

    private PaymentRequestResponse approveAndQueue(PaymentRequestResponse response, String actor) {
        if (response.getStatus() == null
                || response.getStatus() == PaymentRequestStatus.DRAFT
                || response.getStatus() == PaymentRequestStatus.PENDING_APPROVAL
                || response.getStatus() == PaymentRequestStatus.SUBMITTED) {
            payments.markApproved(response.getId(), actor);
            response = payments.getById(response.getId());
        }
        if (response.getStatus() == PaymentRequestStatus.APPROVED) {
            paymentQueue.queueAfterApproval(response.getId(), response.getRequestNo(), actor);
            response = payments.getById(response.getId());
        }
        return response;
    }

    private Map<String, Object> ensureMembershipCoverageInvoice(
            String providerTenant,
            String serviceId,
            String claimId,
            Map<String, Object> claim,
            FuneralServiceClaimEntity localLink
    ) {
        String fsi = qualified(providerTenant, "funeral_service_invoice");
        String invoice = qualified(providerTenant, "invoice");
        List<Map<String, Object>> invoices = jdbc.queryForList(
                "SELECT fsi.invoice_id, fsi.partner_id, fsi.amount_cents, i.invoice_no"
                        + " FROM " + fsi + " fsi JOIN " + invoice + " i ON i.id=fsi.invoice_id"
                        + " WHERE fsi.funeral_service_id=? AND fsi.membership_claim_id=?"
                        + " ORDER BY fsi.created_at LIMIT 1",
                serviceId, claimId);
        if (!invoices.isEmpty()) return invoices.get(0);

        String debtorPartnerId = resolveMembershipCoverageDebtor(providerTenant, claimId, claim, localLink);
        return createCoverageInvoice(
                providerTenant,
                serviceId,
                "BURIAL_SOCIETY",
                debtorPartnerId,
                claimId,
                null,
                approvedAmount(claim),
                "Membership funeral cover settlement",
                membershipHolderName(claimId),
                membershipHolderIdentity(claimId));
    }

    private Map<String, Object> ensureGroupSocietyCoverageInvoice(
            String serviceId,
            String claimId,
            Map<String, Object> claim,
            long amountCents
    ) {
        List<Map<String, Object>> invoices = jdbc.queryForList("""
                SELECT fsi.invoice_id, fsi.partner_id, fsi.amount_cents, i.invoice_no
                  FROM funeral_service_invoice fsi
                  JOIN invoice i ON i.id = fsi.invoice_id
                 WHERE fsi.funeral_service_id=? AND fsi.group_society_claim_id=?
                 ORDER BY fsi.created_at
                 LIMIT 1
                """, serviceId, claimId);
        if (!invoices.isEmpty()) return invoices.get(0);

        return createCoverageInvoice(
                TenantContext.getCurrentTenant(),
                serviceId,
                "GROUP_SOCIETY",
                Objects.toString(claim.get("society_partner_id"), null),
                null,
                claimId,
                amountCents,
                "Group society funeral cover settlement",
                firstNonBlank(Objects.toString(claim.get("society_name"), null), Objects.toString(claim.get("group_no"), null)),
                Objects.toString(claim.get("group_no"), null));
    }

    private Map<String, Object> createCoverageInvoice(
            String tenant,
            String serviceId,
            String entityType,
            String debtorPartnerId,
            String membershipClaimId,
            String groupSocietyClaimId,
            long amountCents,
            String notes,
            String holderName,
            String holderIdentity
    ) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalStateException("Funeral service reference is missing");
        }
        if (debtorPartnerId == null || debtorPartnerId.isBlank()) {
            throw new IllegalStateException("Cover debtor partner is not configured for the funeral invoice");
        }
        if (amountCents <= 0) {
            throw new IllegalStateException("Funeral cover amount must be greater than zero");
        }

        String service = qualified(tenant, "funeral_service");
        String invoice = qualified(tenant, "invoice");
        String invoiceLine = qualified(tenant, "invoice_line");
        String fsi = qualified(tenant, "funeral_service_invoice");
        Map<String, Object> svc = jdbc.queryForMap(
                "SELECT service_request_no, deceased_name, deceased_identity_number, funeral_date FROM "
                        + service + " WHERE id=?", serviceId);

        String invoiceId = UUID.randomUUID().toString();
        String invoiceNo = generateInvoiceNo(tenant);
        java.sql.Date today = java.sql.Date.valueOf(LocalDate.now());
        java.sql.Date dueDate = svc.get("funeral_date") instanceof java.sql.Date date ? date : today;
        jdbc.update("INSERT INTO " + invoice
                        + "(id,invoice_no,external_ref,source_type,source_id,partner_id,invoice_date,due_date,status,"
                        + "subtotal_cents,tax_cents,discount_cents,total_cents,paid_cents,balance_cents,currency,notes,created_at)"
                        + " VALUES(?,?,?,'FUNERAL_SERVICE',?,?,?,?, 'ISSUED',?,0,0,?,0,?,'ZAR',?,CURRENT_TIMESTAMP)",
                invoiceId, invoiceNo, Objects.toString(svc.get("service_request_no"), serviceId), serviceId,
                debtorPartnerId, today, dueDate, amountCents, amountCents, amountCents, notes);
        jdbc.update("INSERT INTO " + invoiceLine
                        + "(id,invoice_id,description,quantity,show_amount,unit_price_cents,discount_cents,tax_cents,subtotal_cents,total_cents,created_at)"
                        + " VALUES(?,?,?,1,1,?,0,0,?,?,CURRENT_TIMESTAMP)",
                UUID.randomUUID().toString(), invoiceId, "FUNERAL SERVICE COVER", amountCents, amountCents, amountCents);
        jdbc.update("INSERT INTO " + fsi
                        + "(id,funeral_service_id,invoice_id,entity_type,partner_id,membership_claim_id,group_society_claim_id,"
                        + "amount_cents,membership_holder_name,membership_holder_identity,deceased_name,deceased_identity,"
                        + "provider_tenant_id,cover_tenant_id,created_at)"
                        + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                UUID.randomUUID().toString(), serviceId, invoiceId, entityType, debtorPartnerId,
                membershipClaimId, groupSocietyClaimId, amountCents, holderName, holderIdentity,
                svc.get("deceased_name"), svc.get("deceased_identity_number"), tenant, TenantContext.getCurrentTenant());

        return Map.of("invoice_id", invoiceId, "invoice_no", invoiceNo, "partner_id", debtorPartnerId, "amount_cents", amountCents);
    }

    private String generateInvoiceNo(String tenant) {
        String currentTenant = TenantContext.getCurrentTenant();
        if (tenant != null && tenant.equals(currentTenant)) {
            try {
                return numberRangeService.generateNumber("INVOICE");
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "INVOICE number range is not configured for funeral invoices in tenant " + tenant, exception);
            }
        }
        try {
            String routine = qualifiedRoutine(tenant, "generateNumber");
            String invoiceNo = jdbc.queryForObject("SELECT " + routine + "(?)", String.class, "INVOICE");
            if (isBlank(invoiceNo)) {
                throw new IllegalStateException("INVOICE number range returned an empty number");
            }
            return invoiceNo;
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Unable to allocate an INVOICE number from funeral initiator tenant " + tenant, exception);
        }
    }

    private String resolveMembershipCoverageDebtor(
            String providerTenant,
            String claimId,
            Map<String, Object> claim,
            FuneralServiceClaimEntity localLink
    ) {
        if (localLink != null && localLink.getBurialSocietyPartnerId() != null
                && !localLink.getBurialSocietyPartnerId().isBlank()) {
            return localLink.getBurialSocietyPartnerId();
        }
        List<String> linkedDebtors = jdbc.query(
                "SELECT burial_society_partner_id FROM " + qualified(providerTenant, "funeral_service_claim")
                        + " WHERE membership_claim_id=? AND burial_society_partner_id IS NOT NULL LIMIT 1",
                (rs, rowNum) -> rs.getString(1), claimId);
        if (!linkedDebtors.isEmpty() && linkedDebtors.get(0) != null && !linkedDebtors.get(0).isBlank()) {
            return linkedDebtors.get(0);
        }
        if (providerTenant.equals(TenantContext.getCurrentTenant())) {
            List<String> memberIds = jdbc.query(
                    "SELECT member_id FROM membership WHERE id=? LIMIT 1",
                    (rs, rowNum) -> rs.getString(1), Objects.toString(claim.get("membership_id"), null));
            if (!memberIds.isEmpty()) return memberIds.get(0);
        }
        throw new IllegalStateException("Burial society partner mapping is missing for claim " + claimId);
    }

    private String resolveConfiguredFuneralClaimSupplierId() {
        String supplierPartnerId = settingService.getSetting(
                FUNERAL_CLAIM_SUPPLIER_ATTRIBUTE, FUNERAL_CLAIM_PAY_SETTING);
        if (isBlank(supplierPartnerId)) {
            throw new IllegalStateException(
                    "Funeral claim payment supplier is not configured. Configure it under System Configuration > Funeral Claim Payments.");
        }
        supplierPartnerId = supplierPartnerId.trim();
        Integer supplierCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM partner_role WHERE partner=? AND UPPER(TRIM(role))='SUPPLIER'",
                Integer.class, supplierPartnerId);
        if (supplierCount == null || supplierCount == 0) {
            throw new IllegalStateException(
                    "Configured funeral claim payment partner is no longer an approved supplier. Update System Configuration > Funeral Claim Payments.");
        }
        return supplierPartnerId;
    }

    private String resolvePartnerName(String tenant, String partnerId) {
        List<String> names = jdbc.query(
                "SELECT TRIM(CONCAT_WS(' ',NULLIF(name2,''),NULLIF(name3,''),NULLIF(name1,''))) FROM "
                        + qualified(tenant, "partner") + " WHERE id=?",
                (rs, rowNum) -> rs.getString(1), partnerId);
        return names.isEmpty() || names.get(0) == null || names.get(0).isBlank()
                ? "Funeral service provider" : names.get(0).trim();
    }

    private void applySupplierBanking(String tenant, String partnerId, PaymentRequestCreateRequest request) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT bank_name,account_holder,account_number,branch_code,account_type FROM "
                        + qualified(tenant, "partner_bank_account")
                        + " WHERE partner=? AND status='ACTIVE'"
                        + " AND (valid_from IS NULL OR valid_from<=CURRENT_DATE)"
                        + " AND (valid_to IS NULL OR valid_to>=CURRENT_DATE)"
                        + " ORDER BY valid_from DESC,id LIMIT 1",
                partnerId);
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Configured funeral claim supplier banking details are missing, unapproved, expired or not yet valid.");
        }
        Map<String, Object> bank = rows.get(0);
        String bankName = Objects.toString(bank.get("bank_name"), null);
        String accountNumber = Objects.toString(bank.get("account_number"), null);
        String accountType = Objects.toString(bank.get("account_type"), null);
        if (isBlank(bankName) || isBlank(accountNumber) || isBlank(accountType)) {
            throw new IllegalStateException(
                    "Configured funeral claim supplier banking details are incomplete. Complete and approve the supplier banking details before settling funeral claims.");
        }
        request.setPaymentMethod(PaymentMethod.EFT);
        request.setBankName(bankName);
        request.setAccountHolder(firstNonBlank(Objects.toString(bank.get("account_holder"), null), request.getPayeeName()));
        request.setAccountNumber(accountNumber);
        request.setBranchCode(Objects.toString(bank.get("branch_code"), null));
        request.setAccountType(accountType);
    }

    private String membershipHolderName(String claimId) {
        List<String> names = jdbc.query("""
                SELECT TRIM(CONCAT_WS(' ',NULLIF(p.name2,''),NULLIF(p.name3,''),NULLIF(p.name1,'')))
                  FROM membership_claim c
                  JOIN membership m ON m.id=c.membership_id
                  JOIN partner p ON p.id=m.member_id
                 WHERE c.id=?
                """, (rs, rowNum) -> rs.getString(1), claimId);
        return names.isEmpty() ? null : names.get(0);
    }

    private String membershipHolderIdentity(String claimId) {
        List<String> identities = jdbc.query("""
                SELECT pi.value
                  FROM membership_claim c
                  JOIN membership m ON m.id=c.membership_id
                  JOIN partner_identity pi ON pi.partner=m.member_id
                 WHERE c.id=?
                 ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END,
                          pi.type,pi.value
                 LIMIT 1
                """, (rs, rowNum) -> rs.getString(1), claimId);
        return identities.isEmpty() ? null : identities.get(0);
    }

    private long approvedAmount(Map<String, Object> claim) {
        Object approvedValue = claim.get("approved_amount_cents");
        if (approvedValue != null) {
            return Math.max(0L, number(approvedValue));
        }
        return Math.max(0L, number(claim.get("claim_amount_cents")));
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String qualified(String tenant, String table) {
        if (tenant == null || !tenant.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("Invalid provider tenant");
        }
        return "`" + tenant + "`.`" + table + "`";
    }

    private String qualifiedRoutine(String tenant, String routine) {
        if (tenant == null || !tenant.matches("[A-Za-z0-9_-]{1,128}")
                || routine == null || !routine.matches("[A-Za-z0-9_]{1,128}")) {
            throw new IllegalArgumentException("Invalid tenant routine");
        }
        return "`" + tenant + "`." + routine;
    }

    private String effectiveActor(String actor) {
        return actor == null || actor.isBlank() ? "SYSTEM" : actor;
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
