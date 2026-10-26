package za.co.mawa.bes.service.v2.tombstone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimCreateRequest;
import za.co.mawa.bes.dto.v2.membership.claim.MembershipClaimResponse;
import za.co.mawa.bes.dto.v2.tombstone.TombstoneDtos;
import za.co.mawa.bes.dto.v2.funeral.FuneralMembershipCoverDto;
import za.co.mawa.bes.dto.v2.funeral.FuneralTenantIntegrationConfigDto;
import za.co.mawa.bes.dto.v2.tombstone.TombstoneInstallationDtos;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.enums.MembershipClaimDeceasedType;
import za.co.mawa.bes.enums.MembershipClaimStatus;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.repository.v2.MembershipClaimRepository;
import za.co.mawa.bes.service.v2.ApprovalService;
import za.co.mawa.bes.service.v2.CrossTenantExecutionService;
import za.co.mawa.bes.service.v2.FuneralManagementService;
import za.co.mawa.bes.service.v2.ExternalFuneralClaimApprovalService;
import za.co.mawa.bes.service.v2.MembershipClaimService;
import za.co.mawa.bes.service.v2.NumberAllocationService;
import za.co.mawa.bes.service.v2.claim.ClaimFormGenerationService;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class TombstoneInstallationRequestService {
    private static final Logger log = LoggerFactory.getLogger(TombstoneInstallationRequestService.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final NumberAllocationService numbers;
    private final MembershipClaimService claims;
    private final MembershipClaimRepository claimRepository;
    private final FuneralManagementService funeral;
    private final CrossTenantExecutionService crossTenant;
    private final TombstoneManagementService tombstones;
    private final ApprovalService approvals;
    private final ClaimFormGenerationService forms;
    private final ExternalFuneralClaimApprovalService externalApprovals;

    @Transactional
    public Map<String, Object> create(TombstoneInstallationDtos.Request payload, String actor) {
        validate(payload);
        String id = UUID.randomUUID().toString();
        String number = numbers.allocateNumber("TOMBSTONE_INSTALLATION_REQUEST");
        jdbc.update("""
            INSERT INTO tombstone_installation_request
              (id, request_no, status, customer_partner_id, deceased_partner_id, deceased_name,
               deceased_identity_number, date_of_death, cause_of_death, death_certificate_no,
               family_representative_partner_id, family_representative_name, family_representative_contact,
               cemetery_name, cemetery_area, grave_number, sales_area, installation_directions, workcenter_id,
               expected_installation_date, notes, discount_cents, items_json, covers_json, created_by, updated_by)
            VALUES (?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, id, number, required(payload.getCustomerPartnerId(), "customerPartnerId"),
                trim(payload.getDeceasedPartnerId()),
                required(payload.getDeceasedName(), "deceasedName"), trim(payload.getDeceasedIdentityNumber()),
                payload.getDateOfDeath(), trim(payload.getCauseOfDeath()), trim(payload.getDeathCertificateNo()),
                trim(payload.getFamilyRepresentativePartnerId()), trim(payload.getFamilyRepresentativeName()),
                trim(payload.getFamilyRepresentativeContact()), trim(payload.getCemeteryName()),
                trim(payload.getCemeteryArea()), trim(payload.getGraveNumber()), trim(payload.getSalesArea()),
                trim(payload.getInstallationDirections()), trim(payload.getWorkcenterId()), payload.getExpectedInstallationDate(), trim(payload.getNotes()),
                payload.getDiscountCents() == null ? 0L : payload.getDiscountCents(),
                json(payload.getItems()), json(payload.getSelectedCovers() == null ? List.of() : payload.getSelectedCovers()), actor(actor), actor(actor));
        return get(id);
    }

    @Transactional
    public Map<String, Object> update(String id, TombstoneInstallationDtos.Request payload, String actor) {
        validate(payload);
        Map<String, Object> existing = locked(id);
        ensureEditable(existing);
        jdbc.update("""
            UPDATE tombstone_installation_request SET customer_partner_id=?, deceased_partner_id=?,
                deceased_name=?, deceased_identity_number=?, date_of_death=?, cause_of_death=?,
                death_certificate_no=?, family_representative_partner_id=?, family_representative_name=?,
                family_representative_contact=?, cemetery_name=?, cemetery_area=?, grave_number=?,
                sales_area=?, installation_directions=?, workcenter_id=?, expected_installation_date=?, notes=?, discount_cents=?,
                items_json=?, covers_json=?, updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE id=?
            """, required(payload.getCustomerPartnerId(), "customerPartnerId"),
                trim(payload.getDeceasedPartnerId()),
                required(payload.getDeceasedName(), "deceasedName"), trim(payload.getDeceasedIdentityNumber()),
                payload.getDateOfDeath(), trim(payload.getCauseOfDeath()), trim(payload.getDeathCertificateNo()),
                trim(payload.getFamilyRepresentativePartnerId()), trim(payload.getFamilyRepresentativeName()),
                trim(payload.getFamilyRepresentativeContact()), trim(payload.getCemeteryName()),
                trim(payload.getCemeteryArea()), trim(payload.getGraveNumber()), trim(payload.getSalesArea()),
                trim(payload.getInstallationDirections()), trim(payload.getWorkcenterId()), payload.getExpectedInstallationDate(), trim(payload.getNotes()),
                payload.getDiscountCents() == null ? 0L : payload.getDiscountCents(),
                json(payload.getItems()), json(payload.getSelectedCovers() == null ? List.of() : payload.getSelectedCovers()), actor(actor), id);
        return get(id);
    }

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("""
            SELECT id, request_no, status, deceased_name, date_of_death, cemetery_name,
                   expected_installation_date, tombstone_order_id, created_at
              FROM tombstone_installation_request ORDER BY created_at DESC
            """);
    }

    public Map<String, Object> get(String id) {
        Map<String, Object> result = new LinkedHashMap<>(one(id));
        result.put("items", readJson(result.remove("items_json")));
        result.put("selectedCovers", readJson(result.remove("covers_json")));
        result.put("claims", linkedClaims(id));
        return result;
    }

    /**
     * Discover memberships through the approved local/external funeral integration,
     * but evaluate TOMBSTONE entitlements in the membership-owning tenant.
     *
     * A previously swallowed error could make a valid external membership look
     * like it did not exist. Never advertise an unverified benefit, but return a
     * useful error when memberships are found and entitlement checks fail.
     */
    public List<Map<String, Object>> checkMembership(String identityNumber, LocalDate eventDate) {
        required(identityNumber, "identityNumber");
        List<FuneralMembershipCoverDto> matched = funeral.checkMembership(identityNumber);
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        Set<String> processed = new HashSet<>();
        for (FuneralMembershipCoverDto cover : matched) {
            String owner = StringUtils.hasText(cover.getSourceTenantId())
                    ? cover.getSourceTenantId() : currentTenant();
            String membershipId = cover.getSourceMembershipId();
            String deceasedPartner = cover.getDeceasedPartnerId();
            if (!StringUtils.hasText(membershipId) || !StringUtils.hasText(deceasedPartner)) {
                log.warn("TOMBSTONE cover lookup received an incomplete candidate from source tenant {}", owner);
                failures.add("A membership source returned incomplete deceased or membership details");
                continue;
            }
            if (!processed.add(owner + ":" + membershipId + ":" + deceasedPartner)) continue;
            try {
                Map<String, Object> entitlement = inOwner(owner, null, () -> claims.resolveBenefit(
                        membershipId, MembershipClaimType.TOMBSTONE, deceasedPartner,
                        eventDate == null ? LocalDate.now() : eventDate, null));
                Object rawAmount = entitlement.get("claimAmountCents");
                long amount = rawAmount instanceof Number ? ((Number) rawAmount).longValue() : 0L;
                if (amount <= 0L) {
                    failures.add("The membership has no positive TOMBSTONE payout for this deceased");
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("membershipId", membershipId);
                item.put("membershipNumber", cover.getMembershipNumber());
                item.put("burialSocietyName", cover.getBurialSocietyName());
                item.put("tenantId", owner);
                item.put("sourceTenantName", cover.getSourceTenantName());
                item.put("deceasedPartnerId", deceasedPartner);
                item.put("deceasedType", cover.getDeceasedType());
                item.put("tombstoneAmountCents", amount);
                result.add(item);
            } catch (RuntimeException exception) {
                // Log the technical cause for administrators without logging the identity number.
                log.warn("TOMBSTONE entitlement lookup failed in source tenant {} for membership {}: {}",
                        owner, membershipId, exception.getMessage(), exception);
                failures.add(explainCoverFailure(exception));
            }
        }
        if (result.isEmpty() && !failures.isEmpty()) {
            // Preserve the safety boundary: never claim another tenant's unverified benefit.
            throw new IllegalArgumentException(
                    "Membership found, but the TOMBSTONE benefit cannot be used: " +
                    String.join("; ", new LinkedHashSet<>(failures)));
        }
        if (matched.isEmpty()) {
            log.info("TOMBSTONE cover lookup returned no memberships in enabled local/external sources for tenant {}",
                    currentTenant());
        }
        return result;
    }

    private String explainCoverFailure(RuntimeException exception) {
        String message = Objects.toString(exception.getMessage(), "").toLowerCase(Locale.ROOT);
        if (message.contains("claim type") && message.contains("not enabled")) {
            return "The membership source uses direct-claim configuration when resolving a benefit; update the backend benefit validation";
        }
        if (message.contains("no payout rule configured")) {
            return "Configure an active TOMBSTONE payout rule for the deceased's membership plan and dependent type in the membership-owning tenant";
        }
        if (message.contains("not linked to the membership")) {
            return "The deceased is not linked to the source membership";
        }
        if (message.contains("membership not found")) {
            return "The membership could not be resolved in its owning tenant";
        }
        return "The membership-owning tenant could not verify the TOMBSTONE entitlement (check backend logs)";
    }

    /** Requests are separately persisted before creating claims in a source tenant. */
    public Map<String, Object> initiateClaims(String id, String actor) {
        Map<String, Object> request = one(id);
        if (!Set.of("DRAFT", "CLAIMS_INITIATED").contains(asString(request.get("status")))) {
            throw new IllegalStateException("Claims cannot be initiated in the request's current status");
        }
        validateOrderReadiness(request);
        List<TombstoneInstallationDtos.SelectedCover> covers = readCovers(request.get("covers_json"));
        if (covers.isEmpty()) throw new IllegalArgumentException("No Tombstone Claim is required for this customer-funded request");
        if (toDate(request.get("date_of_death")) == null) {
            throw new IllegalArgumentException("Date of death is required to initiate Tombstone Claims");
        }
        requireCoverLimit(covers.size());
        Set<String> unique = new HashSet<>();
        String providerTenant = currentTenant();
        for (TombstoneInstallationDtos.SelectedCover cover : covers) {
            String owner = StringUtils.hasText(cover.getTenantId()) ? cover.getTenantId().trim() : providerTenant;
            String membershipId = required(cover.getMembershipId(), "membershipId");
            String deceasedPartner = required(cover.getDeceasedPartnerId(), "cover.deceasedPartnerId");
            if (!unique.add(owner + ":" + membershipId)) {
                throw new IllegalArgumentException("The same membership has been selected more than once");
            }
            // Different tenant databases may have different partner IDs for one person.
            // The selected source relationship is checked against that tenant's identity.

            if (!owner.equals(providerTenant)) funeral.requireTrustedTombstoneClaimTenant(owner);
            // A claim's owning tenant controls the policy, validation and benefit amount.
            MembershipClaimResponse claim = inOwner(owner, actor, () -> createOrReuseClaim(
                    id, membershipId, deceasedPartner, cover, request, actor));
            // Unique (request, owning tenant, membership) makes linking idempotent.
            jdbc.update("""
                INSERT INTO tombstone_installation_request_claim
                  (id, request_id, claim_tenant_id, membership_id, membership_claim_id, claim_no)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE membership_claim_id=VALUES(membership_claim_id), claim_no=VALUES(claim_no)
                """, UUID.randomUUID().toString(), id, owner, membershipId, claim.getId(), claim.getClaimNo());
        }
        jdbc.update("UPDATE tombstone_installation_request SET status='CLAIMS_INITIATED', updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='DRAFT'", actor(actor), id);
        return get(id);
    }

    @Transactional
    public MembershipClaimResponse createOrReuseClaim(String requestId, String membershipId,
            String deceasedPartnerId, TombstoneInstallationDtos.SelectedCover selection,
            Map<String, Object> request, String actor) {
        var previous = claimRepository.findByTombstoneInstallationRequestIdAndMembershipId(requestId, membershipId);
        if (previous.isPresent()) {
            if (previous.get().getStatus() == MembershipClaimStatus.CANCELLED) {
                throw new IllegalStateException("A cancelled Tombstone Claim cannot be recreated on this request. Open a new request.");
            }
            return claims.getById(previous.get().getId());
        }
        MembershipClaimCreateRequest create = new MembershipClaimCreateRequest();
        create.setMembershipId(membershipId);
        create.setTombstoneInstallationRequestId(requestId);
        create.setClaimType(MembershipClaimType.TOMBSTONE);
        create.setDeceasedPartnerId(deceasedPartnerId);
        create.setDeceasedType(MembershipClaimDeceasedType.valueOf(required(selection.getDeceasedType(), "deceasedType").toUpperCase(Locale.ROOT)));
        create.setDateOfDeath(toDate(request.get("date_of_death")));
        create.setClaimDate(LocalDate.now());
        create.setCauseOfDeath(asString(request.get("cause_of_death")));
        // A death certificate is not collected for a Tombstone Installation Request.
        // Supporting information is supplied by the family or the membership-owning tenant.
        create.setNotes("Tombstone Installation Request: " + requestId);
        create.setSubmit(false);
        if (StringUtils.hasText(asString(request.get("deceased_identity_number")))) {
            Integer identityMatch = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM partner_identity
                     WHERE partner=? AND value=?
                    """, Integer.class, deceasedPartnerId, asString(request.get("deceased_identity_number")));
            if (identityMatch == null || identityMatch == 0) {
                throw new IllegalArgumentException("Selected membership belongs to a different deceased identity");
            }
        } else if (!Objects.equals(deceasedPartnerId, request.get("deceased_partner_id"))) {
            throw new IllegalArgumentException("A deceased identity number is required to match covers from different tenants");
        }
        // Validate benefit before insertion, to avoid an invalid claim on a source tenant.
        Map<String, Object> entitlement = claims.resolveBenefit(membershipId, MembershipClaimType.TOMBSTONE,
                deceasedPartnerId, toDate(request.get("date_of_death")), actor);
        if (((Number) entitlement.get("claimAmountCents")).longValue() <= 0L) {
            throw new IllegalArgumentException("The selected membership has no TOMBSTONE benefit for the deceased");
        }
        MembershipClaimResponse result = claims.createFromTombstoneInstallationRequest(create, actor);
        return result;
    }

    public List<Map<String, Object>> linkedClaims(String requestId) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                SELECT membership_claim_id, membership_id, claim_no, claim_tenant_id
                FROM tombstone_installation_request_claim WHERE request_id=? ORDER BY created_at
                """, requestId)) {
            String owner = asString(row.get("claim_tenant_id"));
            try {
                MembershipClaimResponse claim = inOwner(owner, null, () -> claims.getById(asString(row.get("membership_claim_id"))));
                Map<String, Object> view = new LinkedHashMap<>(row);
                view.put("status", claim.getStatus());
                view.put("claimAmountCents", claim.getClaimAmountCents());
                view.put("approvedAmountCents", claim.getApprovedAmountCents());
                view.put("deceasedPartnerId", claim.getDeceasedPartnerId());
                results.add(view);
            } catch (RuntimeException exception) {
                Map<String, Object> view = new LinkedHashMap<>(row);
                view.put("status", "SOURCE_UNAVAILABLE");
                results.add(view);
            }
        }
        return results;
    }

    public byte[] claimForm(String requestId, String claimId, String actor) {
        Map<String, Object> link = requireLinkedClaim(requestId, claimId);
        return inOwner(asString(link.get("claim_tenant_id")), actor, () -> {
            byte[] result = forms.generatePdf(claimId);
            claims.recordClaimFormDownload(claimId, actor);
            return result;
        });
    }

    public Map<String, Object> submitClaim(String requestId, String claimId, String actor) {
        Map<String, Object> link = requireLinkedClaim(requestId, claimId);
        String owner = asString(link.get("claim_tenant_id"));
        if (!Objects.equals(owner, currentTenant())) {
            funeral.requireTrustedTombstoneClaimTenant(owner);
        }
        String providerTenant = currentTenant();
        Integer supporting = jdbc.queryForObject("""
                SELECT COUNT(*) FROM attachment
                 WHERE object_id=? AND UPPER(COALESCE(document_type,'')) <> 'CLAIM-FORM'
                """, Integer.class, claimId);
        if (supporting == null || supporting == 0) {
            throw new IllegalArgumentException("Attach supporting documents before submitting the Tombstone Claim");
        }
        return inOwner(owner, actor, () -> {
            MembershipClaimResponse claim = claims.getById(claimId);
            if (claim.getClaimType() != MembershipClaimType.TOMBSTONE) throw new IllegalStateException("Not a Tombstone Claim");
            if (claim.getClaimFormDownloadCount() == null || claim.getClaimFormDownloadCount() < 1) {
                throw new IllegalStateException("Download the Tombstone Claim form before approval submission");
            }
            if (claim.getStatus() != MembershipClaimStatus.DRAFT) throw new IllegalStateException("Only draft claims may be submitted");
            ApprovalSubmitRequest submission = new ApprovalSubmitRequest();
            submission.setApprovalType(ApprovalType.CLAIM_TOMBSTONE);
            submission.setReferenceId(claimId);
            submission.setReferenceNo(claim.getClaimNo());
            submission.setRequesterId(actor(actor));
            submission.setTitle("Tombstone claim " + claim.getClaimNo());
            submission.setDescription("Verify the deceased, applicable tombstone benefit and supporting documents");
            submission.setPayloadJson(json(Map.of("claimId", claimId, "tombstoneInstallationRequestId", requestId)));
            if (owner.equals(providerTenant)) {
                approvals.submitForApproval(submission);
            } else {
                externalApprovals.submitBatch(providerTenant, List.of(
                    new ExternalFuneralClaimApprovalService.ExternalClaimSubmission(
                        submission, List.of(claimId))));
            }
            return Map.of("claimId", claimId, "status", "SUBMITTED");
        });
    }

    /** Final action in the wizard: only one operational order/invoice per request. */
    @Transactional
    public Map<String, Object> generate(String id, String actor) {
        Map<String, Object> request = locked(id);
        if (StringUtils.hasText(asString(request.get("tombstone_order_id")))) return get(id);
        // A draft may be incomplete, but no invoice or production order may be
        // created until billing, engraving and site details are complete.
        validateOrderReadiness(request);
        List<TombstoneInstallationDtos.SelectedCover> covers = readCovers(request.get("covers_json"));
        boolean claimingBenefit = !covers.isEmpty();
        if (claimingBenefit && !"CLAIMS_INITIATED".equals(asString(request.get("status")))) {
            throw new IllegalStateException("Initiate the selected Tombstone Claims before generating an order");
        }
        if (!claimingBenefit && !"DRAFT".equals(asString(request.get("status")))) {
            throw new IllegalStateException("This request cannot generate a customer-funded order in its current status");
        }
        List<Map<String, Object>> selected = jdbc.queryForList(
                "SELECT membership_claim_id FROM tombstone_installation_request_claim WHERE request_id=?", id);
        if (claimingBenefit && selected.size() != covers.size()) {
            throw new IllegalStateException("Initiate every selected Tombstone Claim before generating an order");
        }
        if (!claimingBenefit && !selected.isEmpty()) {
            throw new IllegalStateException("A request with linked claims cannot generate a customer-funded order");
        }
        TombstoneDtos.CreateOrderRequest order = new TombstoneDtos.CreateOrderRequest();
        order.setCustomerPartnerId(asString(request.get("customer_partner_id")));
        order.setMembershipId(selected.size() == 1 ? findSingleLocalMembership(id) : null);
        order.setDeceasedPartnerId(asString(request.get("deceased_partner_id")));
        order.setDeceasedName(asString(request.get("deceased_name")));
        order.setCemeteryName(asString(request.get("cemetery_name")));
        order.setCemeteryArea(asString(request.get("cemetery_area")));
        order.setGraveNumber(asString(request.get("grave_number")));
        order.setSalesArea(asString(request.get("sales_area")));
        order.setWorkcenterId(asString(request.get("workcenter_id")));
        order.setExpectedInstallationDate(toDate(request.get("expected_installation_date")));
        order.setFundingMethod(claimingBenefit ? "FUNERAL_COVER" : "CASH");
        order.setDiscountCents(((Number) request.get("discount_cents")).longValue());
        String directions = trim(asString(request.get("installation_directions")));
        order.setNotes("Installation request " + request.get("request_no") + ". "
                + Objects.toString(request.get("notes"), "")
                + (directions == null ? "" : "\nInstallation directions: " + directions));
        try {
            order.setItems(mapper.readValue(asString(request.get("items_json")),
                    new TypeReference<List<TombstoneDtos.ItemRequest>>() {}));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid stored tombstone items", exception);
        }
        var generated = tombstones.createOrder(order, actor);
        jdbc.update("UPDATE tombstone_installation_request SET tombstone_order_id=?, status='ORDER_GENERATED', updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                generated.getId(), actor(actor), id);
        return get(id);
    }

    /** Reconcile approved source claims into the provider order, always capped at its unpaid balance. */
    @Transactional
    public Map<String, Object> allocateApprovedFunding(String id, String actor) {
        Map<String, Object> request = locked(id);
        String orderId = asString(request.get("tombstone_order_id"));
        if (!StringUtils.hasText(orderId) || "CANCELLED".equals(asString(request.get("status")))) {
            throw new IllegalStateException("Generate a valid Tombstone Order before applying cover funding");
        }
        var order = tombstones.getOrder(orderId);
        for (Map<String, Object> link : jdbc.queryForList("""
                SELECT membership_claim_id, claim_tenant_id FROM tombstone_installation_request_claim
                 WHERE request_id=? ORDER BY created_at
                """, id)) {
            if (order.getBalanceCents() == null || order.getBalanceCents() <= 0) break;
            String claimId = asString(link.get("membership_claim_id"));
            String tenant = asString(link.get("claim_tenant_id"));
            MembershipClaimResponse claim = inOwner(tenant, actor, () -> claims.getById(claimId));
            if (claim.getStatus() != MembershipClaimStatus.APPROVED) continue;
            long approved = claim.getApprovedAmountCents() == null ? 0 : claim.getApprovedAmountCents();
            if (approved <= 0) continue;
            TombstoneDtos.FundingAllocationRequest funding = new TombstoneDtos.FundingAllocationRequest();
            funding.setFundingType("FUNERAL_COVER");
            funding.setSourceType("MEMBERSHIP_CLAIM");
            funding.setSourceId(claimId);
            funding.setSourceTenantId(tenant);
            funding.setAllocatedAmountCents(Math.min(approved, order.getBalanceCents()));
            order = tombstones.addFunding(orderId, funding, actor);
        }
        return get(id);
    }

    @Transactional
    public Map<String, Object> cancel(String id, String reason, String actor) {
        if (!StringUtils.hasText(reason)) throw new IllegalArgumentException("Cancellation reason is required");
        Map<String, Object> request = locked(id);
        if ("CANCELLED".equals(asString(request.get("status")))) return get(id);
        if (StringUtils.hasText(asString(request.get("tombstone_order_id")))) {
            throw new IllegalStateException("Cancel the generated order through Tombstone Management after financial review");
        }
        // Never cancel another tenant's claim or release an approved benefit implicitly.
        jdbc.update("UPDATE tombstone_installation_request SET status='CANCELLED', cancellation_reason=?, updated_at=CURRENT_TIMESTAMP, updated_by=? WHERE id=?",
                reason.trim(), actor(actor), id);
        return get(id);
    }

    private Map<String, Object> requireLinkedClaim(String requestId, String claimId) {
        List<Map<String, Object>> links = jdbc.queryForList(
                "SELECT * FROM tombstone_installation_request_claim WHERE request_id=? AND membership_claim_id=?", requestId, claimId);
        if (links.isEmpty()) throw new IllegalArgumentException("Claim does not belong to the installation request");
        return links.get(0);
    }

    private String findSingleLocalMembership(String requestId) {
        List<Map<String, Object>> links = jdbc.queryForList(
                "SELECT membership_id, claim_tenant_id FROM tombstone_installation_request_claim WHERE request_id=?", requestId);
        return links.size() == 1 && Objects.equals(links.get(0).get("claim_tenant_id"), currentTenant())
                ? asString(links.get(0).get("membership_id")) : null;
    }

    private Map<String, Object> locked(String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM tombstone_installation_request WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Tombstone Installation Request not found: " + id);
        return rows.get(0);
    }
    private Map<String, Object> one(String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM tombstone_installation_request WHERE id=?", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Tombstone Installation Request not found: " + id);
        return rows.get(0);
    }
    private void ensureEditable(Map<String, Object> request) {
        if (!"DRAFT".equals(request.get("status"))) {
            throw new IllegalStateException("Only draft requests may be edited");
        }
    }
    /** Guarded on the backend as well as in the wizard: direct API requests and
     * old, incomplete drafts must not generate incomplete invoices or engraving jobs.
     * Cemetery, cemetery section, grave number and installation date remain optional.
     */
    private void validateOrderReadiness(Map<String, Object> row) {
        required(asString(row.get("deceased_name")), "deceased full name");
        required(asString(row.get("customer_partner_id")), "invoice customer partner");
        required(asString(row.get("family_representative_name")), "family representative name");
        required(asString(row.get("family_representative_contact")), "family representative contact details");
        required(asString(row.get("sales_area")), "installation area");
        required(asString(row.get("installation_directions")), "directions to installation site");
        List<TombstoneDtos.ItemRequest> items;
        try {
            items = mapper.readValue(required(asString(row.get("items_json")), "items"), new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid tombstone order items", exception);
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Select exactly one Tombstone package");
        }
        List<TombstoneDtos.ItemRequest> packages = items.stream()
                .filter(item -> "TOMBSTONE".equalsIgnoreCase(item.getItemType())).toList();
        if (packages.size() != 1) {
            throw new IllegalArgumentException("Select exactly one Tombstone package per deceased");
        }
        TombstoneDtos.ItemRequest memorial = packages.get(0);
        required(memorial.getProductId(), "Tombstone product for invoice");
        required(memorial.getDescription(), "Tombstone invoice description");
        required(memorial.getInscriptionText(), "exact inscription / engraving wording");
        required(memorial.getMaterial(), "Tombstone material");
        required(memorial.getColour(), "Tombstone colour");
        required(memorial.getDimensions(), "Tombstone dimensions");
        if (memorial.getUnitPriceCents() == null || memorial.getUnitPriceCents() <= 0) {
            throw new IllegalArgumentException("Tombstone package must have a positive price for invoicing");
        }
        long subtotal = 0;
        for (TombstoneDtos.ItemRequest item : items) {
            required(item.getItemType(), "item type");
            required(item.getProductId(), "item product for invoice");
            required(item.getDescription(), "item invoice description");
            if (item.getUnitPriceCents() == null || item.getUnitPriceCents() < 0) {
                throw new IllegalArgumentException("Every invoice item must have a valid price");
            }
            if (item.getQuantity() != null && item.getQuantity().compareTo(java.math.BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Tombstone installation item quantity must be one");
            }
            subtotal = Math.addExact(subtotal, item.getUnitPriceCents());
        }
        long discount = row.get("discount_cents") instanceof Number number ? number.longValue() : 0L;
        if (discount < 0 || discount > subtotal) {
            throw new IllegalArgumentException("Order discount must not exceed the total item price");
        }
    }

    private void validate(TombstoneInstallationDtos.Request request) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        required(request.getCustomerPartnerId(), "customerPartnerId");
        required(request.getDeceasedName(), "deceasedName");
        // Families may commission a tombstone without having used our funeral or membership services.
        // Their supplied deceased details are sufficient: an internal partner record is optional.
        if (request.getDateOfDeath() != null && request.getDateOfDeath().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of death cannot be in the future");
        }
        if (request.getSelectedCovers() != null && !request.getSelectedCovers().isEmpty()
                && request.getDateOfDeath() == null) {
            throw new IllegalArgumentException("Date of death is required only when claiming a membership benefit");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one tombstone product or installation service is required");
        }
        long tombstoneCount = request.getItems().stream()
                .filter(item -> "TOMBSTONE".equalsIgnoreCase(item.getItemType())).count();
        if (tombstoneCount != 1) {
            throw new IllegalArgumentException("Select exactly one Tombstone package per deceased");
        }
        requireCoverLimit(request.getSelectedCovers() == null ? 0 : request.getSelectedCovers().size());
        if (request.getDiscountCents() != null && request.getDiscountCents() < 0) throw new IllegalArgumentException("Discount cannot be negative");
        for (TombstoneDtos.ItemRequest item : request.getItems()) {
            required(item.getDescription(), "item.description");
            if (item.getUnitPriceCents() == null || item.getUnitPriceCents() < 0) throw new IllegalArgumentException("Invalid item unit price");
            if (item.getQuantity() != null
                    && item.getQuantity().compareTo(java.math.BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Tombstone installation item quantity must be one");
            }
        }
    }
    public Map<String, Object> configuration() {
        int maximum = funeral.getServiceConfiguration().getMaxSelectableCovers();
        FuneralTenantIntegrationConfigDto integration = funeral.getTenantIntegrationConfiguration();
        String mode = integration == null || !StringUtils.hasText(integration.getMembershipSourceMode())
                ? "LOCAL_ONLY" : integration.getMembershipSourceMode();
        boolean externalSource = "EXTERNAL_ONLY".equalsIgnoreCase(mode)
                || "LOCAL_AND_EXTERNAL".equalsIgnoreCase(mode);
        boolean externalLookupEnabled = externalSource && integration != null
                && Boolean.TRUE.equals(integration.getActive())
                && Boolean.TRUE.equals(integration.getMembershipLookupEnabled())
                && StringUtils.hasText(integration.getExternalTenantId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxSelectedCovers", maximum);
        result.put("coverLimitEnabled", maximum > 0);
        result.put("membershipSourceMode", mode);
        result.put("externalLookupEnabled", externalLookupEnabled);
        return result;
    }
    private void requireCoverLimit(int selected) {
        int maximum = funeral.getServiceConfiguration().getMaxSelectableCovers();
        if (maximum > 0 && selected > maximum) {
            throw new IllegalArgumentException("Select at most " + maximum + " membership cover(s)");
        }
    }
    private List<TombstoneInstallationDtos.SelectedCover> readCovers(Object json) {
        try {
            if (json == null) return List.of();
            List<TombstoneInstallationDtos.SelectedCover> covers = mapper.readValue(asString(json), new TypeReference<>() {});
            return covers == null ? List.of() : covers;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid saved membership selections", exception);
        }
    }
    private Object readJson(Object json) {
        try { return mapper.readValue(asString(json), Object.class); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Invalid saved request", exception); }
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Invalid request content", exception); }
    }
    private <T> T inOwner(String tenant, String actor, Supplier<T> operation) {
        if (Objects.equals(tenant, currentTenant())) return operation.get();
        return crossTenant.execute(tenant, UserContext.getCurrentUser(), actor, operation);
    }
    private String required(String value, String field) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
    private String trim(String text) { return StringUtils.hasText(text) ? text.trim() : null; }
    private String asString(Object o) { return o == null ? null : o.toString(); }
    private String currentTenant() { return Objects.toString(TenantContext.getCurrentTenant(), "mawa"); }
    private String actor(String actor) { return StringUtils.hasText(actor) ? actor : "SYSTEM"; }
    private LocalDate toDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return value == null ? null : LocalDate.parse(value.toString());
    }
}
