package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.funeral.*;
import za.co.mawa.bes.dto.v2.FuneralPackageCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralPackageUpdateRequestDto;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.repository.v2.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuneralManagementService {

    private static final String COVER_SOURCE_LOCAL = "LOCAL_TENANT";
    private static final String COVER_SOURCE_EXTERNAL = "EXTERNAL_TENANT";

    private final FuneralPickupRequestRepository pickupRequestRepository;
    private final FuneralMortuaryInventoryRepository mortuaryInventoryRepository;
    private final FuneralPackageRepository funeralPackageRepository;
    private final FuneralServiceRepository funeralServiceRepository;
    private final FuneralServiceInvoiceRepository funeralServiceInvoiceRepository;
    private final FuneralServiceClaimRepository funeralServiceClaimRepository;
    private final FuneralExternalMembershipCoverRepository externalMembershipCoverRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<FuneralPickupRequestEntity> getPickupRequests() {
        return pickupRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public FuneralPickupRequestEntity createPickupRequest(CreatePickupRequestDto request) {
        validateRequired(request.getDeceasedName(), "deceasedName");
        validateRequired(request.getPickupLocation(), "pickupLocation");

        FuneralPickupRequestEntity entity = new FuneralPickupRequestEntity();
        entity.setDeceasedName(request.getDeceasedName());
        entity.setPickupLocation(request.getPickupLocation());
        entity.setContactPerson(request.getContactPerson());
        entity.setContactNumber(request.getContactNumber());
        entity.setStatus("PENDING");
        return pickupRequestRepository.save(entity);
    }

    @Transactional
    public FuneralPickupRequestEntity assignPickupRequest(String id, AssignPickupRequestDto request) {
        validateRequired(request.getStaffId(), "staffId");
        FuneralPickupRequestEntity entity = getPickupRequestOrThrow(id);
        entity.setAssignedStaffId(request.getStaffId());
        entity.setStatus("ASSIGNED");
        return pickupRequestRepository.save(entity);
    }

    @Transactional
    public FuneralPickupRequestEntity completePickupRequest(String id, CompletePickupRequestDto request) {
        FuneralPickupRequestEntity pickup = getPickupRequestOrThrow(id);
        if ("COMPLETED".equalsIgnoreCase(pickup.getStatus()) && pickup.getMortuaryInventoryId() != null) {
            return pickup;
        }

        LocalDateTime completionTime = request.getCompletionTime() == null ? LocalDateTime.now() : request.getCompletionTime();
        FuneralMortuaryInventoryEntity inventory = new FuneralMortuaryInventoryEntity();
        inventory.setPickupRequestId(pickup.getId());
        inventory.setDeceasedName(pickup.getDeceasedName());
        inventory.setCheckInDate(completionTime);
        inventory.setStatus("IN_MORTUARY");
        inventory.setTagNumber(generateTagNumber(completionTime));
        inventory = mortuaryInventoryRepository.save(inventory);

        pickup.setCompletionTime(completionTime);
        pickup.setStatus("COMPLETED");
        pickup.setMortuaryInventoryId(inventory.getId());
        return pickupRequestRepository.save(pickup);
    }

    public List<FuneralMortuaryInventoryEntity> getMortuaryInventory() {
        return mortuaryInventoryRepository.findByStatus("IN_MORTUARY");
    }

    @Transactional
    public Map<String, Object> checkoutMortuary(String id, MortuaryCheckoutDto request) {
        FuneralMortuaryInventoryEntity inventory = mortuaryInventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mortuary inventory not found: " + id));
        inventory.setReleaseTo(request.getReleaseTo());
        inventory.setIdentityNumber(request.getIdentityNumber());
        inventory.setCheckoutDate(request.getCheckoutDate() == null ? LocalDateTime.now() : request.getCheckoutDate());
        inventory.setStatus("CHECKED_OUT");
        mortuaryInventoryRepository.save(inventory);
        return Map.of("success", true, "id", inventory.getId(), "status", inventory.getStatus());
    }

    public List<FuneralPackageEntity> getPackages() {
        return getPackages(true);
    }

    public List<FuneralPackageEntity> getPackages(boolean activeOnly) {
        if (activeOnly) {
            return funeralPackageRepository.findByActiveTrue();
        }
        return funeralPackageRepository.findAll();
    }

    public FuneralPackageEntity getPackage(String id) {
        return getFuneralPackageOrThrow(id);
    }

    @Transactional
    public FuneralPackageEntity createPackage(FuneralPackageCreateRequestDto request) {
        validateRequired(request.getName(), "name");
        FuneralPackageEntity entity = new FuneralPackageEntity();
        entity.setName(request.getName().trim());
        entity.setBasePriceCents(defaultLong(request.getBasePriceCents()));
        entity.setInclusionsJson(resolveInclusionsJson(request.getInclusionsJson(), request.getInclusions()));
        entity.setActive(request.getActive() == null || request.getActive());
        return funeralPackageRepository.save(entity);
    }

    @Transactional
    public FuneralPackageEntity updatePackage(String id, FuneralPackageUpdateRequestDto request) {
        validateRequired(id, "id");
        validateRequired(request.getName(), "name");
        FuneralPackageEntity entity = getFuneralPackageOrThrow(id);
        entity.setName(request.getName().trim());
        entity.setBasePriceCents(defaultLong(request.getBasePriceCents()));
        entity.setInclusionsJson(resolveInclusionsJson(request.getInclusionsJson(), request.getInclusions()));
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        return funeralPackageRepository.save(entity);
    }

    @Transactional
    public void deletePackage(String id) {
        FuneralPackageEntity entity = getFuneralPackageOrThrow(id);
        entity.setActive(false);
        funeralPackageRepository.save(entity);
    }


    private String resolveInclusionsJson(String inclusionsJson, List<String> inclusions) {
        if (inclusions != null) {
            try {
                return objectMapper.writeValueAsString(inclusions);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid funeral package inclusions", e);
            }
        }
        if (inclusionsJson == null || inclusionsJson.isBlank()) {
            return "[]";
        }
        try {
            objectMapper.readTree(inclusionsJson);
            return inclusionsJson;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("inclusionsJson must be valid JSON", e);
        }
    }

    private FuneralPackageEntity getFuneralPackageOrThrow(String id) {
        return funeralPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funeral package not found: " + id));
    }

    /**
     * Combines same-tenant cover from existing membership tables with external Mawa tenant cover snapshots.
     * Same-tenant cover uses membership, membership_dependent and membership_plan_claim_payout.
     */
    public List<FuneralMembershipCoverDto> checkMembership(String identityNumber) {
        validateRequired(identityNumber, "identityNumber");
        List<FuneralMembershipCoverDto> result = new ArrayList<>();
        result.addAll(findLocalMembershipCover(identityNumber));
        result.addAll(externalMembershipCoverRepository.findByIdentityNumberAndStatus(identityNumber, "ACTIVE")
                .stream()
                .map(this::toExternalCoverDto)
                .collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public FuneralServiceRequestResponseDto createServiceRequest(FuneralServiceRequestDto request) {
        populateServiceRequestDefaults(request);
        validateRequired(request.getDeceasedName(), "deceasedName");
        validateRequired(request.getPackageId(), "packageId");
        validateRequired(request.getFamilyRepId(), "familyRepId");

        FuneralPackageEntity packageEntity = funeralPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Funeral package not found: " + request.getPackageId()));

        FuneralServiceEntity entity = new FuneralServiceEntity();
        entity.setMortuaryInventoryId(request.getMortuaryInventoryId());
        entity.setDeceasedName(request.getDeceasedName());
        entity.setDeceasedIdentityNumber(request.getDeceasedIdentityNumber());
        entity.setDeceasedPartnerId(resolveDeceasedPartnerId(request));
        entity.setPackageId(request.getPackageId());
        entity.setFamilyRepId(request.getFamilyRepId());
        entity.setFuneralDate(request.getFuneralDate());
        entity.setFuneralArea(request.getFuneralArea());
        entity.setExtrasJson(toJson(request.getExtras()));
        entity.setTotalAmountCents(defaultLong(packageEntity.getBasePriceCents()) + calculateExtrasTotal(request.getExtras()));
        entity.setStatus("ARRANGEMENT_CREATED");
        return toServiceResponse(funeralServiceRepository.save(entity));
    }

    @Transactional
    public List<FuneralClaimDto> initiateClaims(String funeralServiceId, InitiateFuneralClaimsDto request) {
        FuneralServiceEntity service = getFuneralServiceOrThrow(funeralServiceId);
        if (request.getMemberships() == null || request.getMemberships().isEmpty()) {
            throw new IllegalArgumentException("At least one membership selection is required");
        }
        if (service.getDeceasedPartnerId() == null || service.getDeceasedPartnerId().isBlank()) {
            throw new IllegalArgumentException("Funeral service must have deceasedPartnerId before a local membership_claim can be created");
        }

        Map<String, FuneralMembershipCoverDto> coverMap = resolveSelectedCovers(service, request.getMemberships());
        long remaining = defaultLong(service.getTotalAmountCents());
        List<FuneralClaimDto> response = new ArrayList<>();

        for (String selectionId : request.getMemberships()) {
            FuneralMembershipCoverDto cover = coverMap.get(selectionId);
            if (cover == null || remaining <= 0) continue;
            long claimAmount = Math.min(defaultLong(cover.getCoverAmountCents()), remaining);
            if (claimAmount <= 0) continue;

            String membershipClaimId = UUID.randomUUID().toString();
            String claimNo = generateClaimNo();
            String membershipId = COVER_SOURCE_LOCAL.equals(cover.getCoverSource())
                    ? cover.getSourceMembershipId()
                    : createExternalMembershipPlaceholderIfRequired(cover);

            jdbcTemplate.update("""
                    INSERT INTO membership_claim
                    (id, claim_no, membership_id, claim_type, deceased_type, deceased_partner_id, date_of_death,
                     claim_date, cause_of_death, death_certificate_no, claimant_partner_id, claim_amount_cents,
                     status, notes, created_at)
                    VALUES (?, ?, ?, 'FUNERAL', ?, ?, ?, ?, ?, ?, ?, ?, 'SUBMITTED', ?, CURRENT_TIMESTAMP)
                    """,
                    membershipClaimId,
                    claimNo,
                    membershipId,
                    defaultString(cover.getDeceasedType(), "MAIN_MEMBER"),
                    defaultString(cover.getDeceasedPartnerId(), service.getDeceasedPartnerId()),
                    service.getFuneralDate() == null ? LocalDate.now() : service.getFuneralDate(),
                    LocalDate.now(),
                    request.getCauseOfDeath(),
                    request.getDeathCertificateNo(),
                    service.getFamilyRepId(),
                    claimAmount,
                    request.getNotes());

            FuneralServiceClaimEntity link = new FuneralServiceClaimEntity();
            link.setFuneralServiceId(service.getId());
            link.setMembershipClaimId(membershipClaimId);
            link.setCoverSource(cover.getCoverSource());
            link.setSourceTenantId(cover.getSourceTenantId());
            link.setSourceTenantName(cover.getSourceTenantName());
            link.setSourceMembershipId(cover.getSourceMembershipId());
            link.setSourceReference(cover.getSourceReference());
            link.setBurialSocietyPartnerId(cover.getBurialSocietyPartnerId());
            funeralServiceClaimRepository.save(link);

            response.add(readClaimDto(membershipClaimId));
            remaining -= claimAmount;
        }

        service.setStatus("CLAIMS_INITIATED");
        funeralServiceRepository.save(service);
        return response;
    }

    public List<FuneralClaimDto> getClaims(String funeralServiceId) {
        return funeralServiceClaimRepository.findByFuneralServiceId(funeralServiceId)
                .stream()
                .map(link -> readClaimDto(link.getMembershipClaimId()))
                .collect(Collectors.toList());
    }

    /**
     * Convenience endpoint for early testing. In production, the existing approval/payment workflow should update
     * membership_claim, and this endpoint can be removed or restricted to administrators.
     */
    @Transactional
    public FuneralClaimDto decideClaim(String membershipClaimId, ApproveFuneralClaimDto request) {
        String status = request.getStatus() == null ? "APPROVED" : request.getStatus().trim().toUpperCase();
        if (!List.of("SUBMITTED", "APPROVED", "PARTIALLY_APPROVED", "REJECTED", "CANCELLED", "PAID").contains(status)) {
            throw new IllegalArgumentException("Unsupported claim status: " + status);
        }

        Long claimAmount = jdbcTemplate.queryForObject(
                "SELECT claim_amount_cents FROM membership_claim WHERE id = ?",
                Long.class,
                membershipClaimId);
        long approvedAmount = "REJECTED".equals(status) || "CANCELLED".equals(status) ? 0L : defaultLong(request.getApprovedAmountCents());
        if (("APPROVED".equals(status) || "PARTIALLY_APPROVED".equals(status)) && approvedAmount <= 0) {
            approvedAmount = defaultLong(claimAmount);
        }
        if (approvedAmount > defaultLong(claimAmount)) {
            throw new IllegalArgumentException("approvedAmountCents may not exceed claim_amount_cents");
        }
        if (approvedAmount < defaultLong(claimAmount) && "APPROVED".equals(status)) {
            status = "PARTIALLY_APPROVED";
        }

        jdbcTemplate.update("""
                UPDATE membership_claim
                   SET status = ?,
                       approved_amount_cents = ?,
                       rejection_reason = ?,
                       approved_at = CASE WHEN ? IN ('APPROVED','PARTIALLY_APPROVED','PAID','REJECTED','CANCELLED') THEN CURRENT_TIMESTAMP ELSE approved_at END,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """, status, approvedAmount, request.getDecisionNotes(), status, membershipClaimId);

        updateFuneralServiceClaimStatus(membershipClaimId);
        return readClaimDto(membershipClaimId);
    }

    public List<FuneralInvoiceSplitDto> previewInvoiceSplit(FuneralInvoicePreviewRequestDto request) {
        if (request.getFuneralServiceId() != null && !request.getFuneralServiceId().isBlank()) {
            FuneralServiceEntity service = getFuneralServiceOrThrow(request.getFuneralServiceId());
            return buildSplitsFromApprovedClaims(service);
        }

        validateRequired(request.getPackageId(), "packageId");
        validateRequired(request.getFamilyRepId(), "familyRepId");
        FuneralPackageEntity packageEntity = funeralPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Funeral package not found: " + request.getPackageId()));
        long total = defaultLong(packageEntity.getBasePriceCents()) + calculateExtrasTotal(request.getExtras());
        long remaining = total;
        List<FuneralInvoiceSplitDto> splits = new ArrayList<>();

        if (request.getMemberships() != null && !request.getMemberships().isEmpty()) {
            Map<String, FuneralMembershipCoverDto> covers = resolveSelectedCovers(null, request.getMemberships());
            for (String selectionId : request.getMemberships()) {
                FuneralMembershipCoverDto cover = covers.get(selectionId);
                if (cover == null || remaining <= 0) continue;
                long amount = Math.min(defaultLong(cover.getCoverAmountCents()), remaining);
                if (amount <= 0) continue;
                splits.add(FuneralInvoiceSplitDto.builder()
                        .entityName(cover.getBurialSocietyName())
                        .entityType("BURIAL_SOCIETY")
                        .partnerId(cover.getBurialSocietyPartnerId())
                        .amountCents(amount)
                        .description("Estimated funeral cover pending claim approval")
                        .coverSource(cover.getCoverSource())
                        .sourceTenantId(cover.getSourceTenantId())
                        .build());
                remaining -= amount;
            }
        }

        if (remaining > 0) {
            splits.add(FuneralInvoiceSplitDto.builder()
                    .entityName("Family Representative")
                    .entityType("FAMILY_REP")
                    .partnerId(request.getFamilyRepId())
                    .amountCents(remaining)
                    .description("Family payable balance")
                    .build());
        }
        return splits;
    }

    @Transactional
    public GenerateFuneralInvoicesResponseDto generateInvoices(FuneralInvoicePreviewRequestDto request) {
        validateRequired(request.getFuneralServiceId(), "funeralServiceId");
        FuneralServiceEntity service = getFuneralServiceOrThrow(request.getFuneralServiceId());
        List<FuneralInvoiceSplitDto> splits = buildSplitsFromApprovedClaims(service);
        if (splits.isEmpty()) {
            throw new IllegalArgumentException("No invoice splits generated. Resolve claims first or check funeral amount.");
        }

        List<String> invoiceIds = new ArrayList<>();
        for (FuneralInvoiceSplitDto split : splits) {
            String invoiceId = createInvoice(service, split);
            invoiceIds.add(invoiceId);

            FuneralServiceInvoiceEntity link = new FuneralServiceInvoiceEntity();
            link.setFuneralServiceId(service.getId());
            link.setInvoiceId(invoiceId);
            link.setEntityType(split.getEntityType());
            link.setPartnerId(split.getPartnerId());
            link.setMembershipClaimId(split.getMembershipClaimId());
            link.setAmountCents(split.getAmountCents());
            funeralServiceInvoiceRepository.save(link);
        }

        service.setStatus("INVOICED");
        funeralServiceRepository.save(service);
        return GenerateFuneralInvoicesResponseDto.builder()
                .funeralServiceId(service.getId())
                .invoiceIds(invoiceIds)
                .build();
    }

    @Transactional
    public InvoiceSummaryDto captureInvoicePayment(String invoiceId, CaptureInvoicePaymentDto request) {
        validateRequired(invoiceId, "invoiceId");
        if (defaultLong(request.getAmountCents()) <= 0) {
            throw new IllegalArgumentException("amountCents must be greater than zero");
        }

        Map<String, Object> invoice = jdbcTemplate.queryForMap("SELECT * FROM invoice WHERE id = ?", invoiceId);
        long balance = asLong(invoice.get("balance_cents"));
        if (request.getAmountCents() > balance) {
            throw new IllegalArgumentException("Payment amount exceeds invoice balance");
        }

        jdbcTemplate.update("""
                INSERT INTO invoice_payment (id, invoice_id, payment_date, amount_cents, payment_method, reference_no, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?, CURRENT_TIMESTAMP)
                """, UUID.randomUUID().toString(), invoiceId, request.getAmountCents(), request.getPaymentMethod(), request.getReference());

        long newPaid = asLong(invoice.get("paid_cents")) + request.getAmountCents();
        long total = asLong(invoice.get("total_cents"));
        long newBalance = Math.max(0, total - newPaid);
        String status = newBalance == 0 ? "PAID" : "PARTIALLY_PAID";
        jdbcTemplate.update("UPDATE invoice SET paid_cents = ?, balance_cents = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                newPaid, newBalance, status, invoiceId);

        return InvoiceSummaryDto.builder()
                .invoiceId(invoiceId)
                .invoiceNo(String.valueOf(invoice.get("invoice_no")))
                .status(status)
                .totalCents(total)
                .paidCents(newPaid)
                .balanceCents(newBalance)
                .build();
    }

    private List<FuneralMembershipCoverDto> findLocalMembershipCover(String identityNumber) {
        String mainMemberSql = """
                SELECT CONCAT('LOCAL:', m.id, ':', p.id, ':MAIN_MEMBER') AS selection_id,
                       m.id AS membership_id,
                       m.membership_no AS membership_no,
                       p.id AS deceased_partner_id,
                       'MAIN_MEMBER' AS deceased_type,
                       COALESCE(MAX(pay.payout_amount_cents), 0) AS cover_amount_cents,
                       COALESCE(mp.name, 'Burial Society') AS burial_society_name,
                       NULL AS burial_society_partner_id
                  FROM partner_identity pi
                  JOIN partner p ON p.id = pi.partner
                  JOIN membership m ON m.member_id = p.id
                  JOIN membership_plan mp ON mp.id = m.plan_id
             LEFT JOIN membership_plan_claim_payout pay ON pay.plan_id = m.plan_id
                       AND pay.claim_type = 'FUNERAL'
                       AND pay.active = 1
                       AND pay.dependent_type IN ('MAIN_MEMBER', 'ANY')
                 WHERE pi.value = ?
                   AND m.status = 'ACTIVE'
              GROUP BY m.id, m.membership_no, p.id, mp.name
                """;
        String dependentSql = """
                SELECT CONCAT('LOCAL:', m.id, ':', p.id, ':DEPENDENT') AS selection_id,
                       m.id AS membership_id,
                       m.membership_no AS membership_no,
                       p.id AS deceased_partner_id,
                       'DEPENDENT' AS deceased_type,
                       COALESCE(MAX(pay.payout_amount_cents), 0) AS cover_amount_cents,
                       COALESCE(mp.name, 'Burial Society') AS burial_society_name,
                       NULL AS burial_society_partner_id
                  FROM partner_identity pi
                  JOIN partner p ON p.id = pi.partner
                  JOIN membership_dependent md ON md.dependent_partner_id = p.id AND md.active = 1
                  JOIN membership m ON m.id = md.membership_id
                  JOIN membership_plan mp ON mp.id = m.plan_id
             LEFT JOIN membership_plan_claim_payout pay ON pay.plan_id = m.plan_id
                       AND pay.claim_type = 'FUNERAL'
                       AND pay.active = 1
                       AND pay.dependent_type IN (md.relationship, 'DEPENDENT', 'ANY')
                 WHERE pi.value = ?
                   AND m.status = 'ACTIVE'
              GROUP BY m.id, m.membership_no, p.id, mp.name
                """;

        List<FuneralMembershipCoverDto> covers = new ArrayList<>();
        covers.addAll(jdbcTemplate.query(mainMemberSql, (rs, rowNum) -> FuneralMembershipCoverDto.builder()
                .membershipId(rs.getString("selection_id"))
                .sourceMembershipId(rs.getString("membership_id"))
                .membershipNumber(rs.getString("membership_no"))
                .deceasedPartnerId(rs.getString("deceased_partner_id"))
                .deceasedType(rs.getString("deceased_type"))
                .coverAmountCents(rs.getLong("cover_amount_cents"))
                .burialSocietyName(rs.getString("burial_society_name"))
                .burialSocietyPartnerId(rs.getString("burial_society_partner_id"))
                .coverSource(COVER_SOURCE_LOCAL)
                .build(), identityNumber));
        covers.addAll(jdbcTemplate.query(dependentSql, (rs, rowNum) -> FuneralMembershipCoverDto.builder()
                .membershipId(rs.getString("selection_id"))
                .sourceMembershipId(rs.getString("membership_id"))
                .membershipNumber(rs.getString("membership_no"))
                .deceasedPartnerId(rs.getString("deceased_partner_id"))
                .deceasedType(rs.getString("deceased_type"))
                .coverAmountCents(rs.getLong("cover_amount_cents"))
                .burialSocietyName(rs.getString("burial_society_name"))
                .burialSocietyPartnerId(rs.getString("burial_society_partner_id"))
                .coverSource(COVER_SOURCE_LOCAL)
                .build(), identityNumber));
        return covers;
    }

    private FuneralMembershipCoverDto toExternalCoverDto(FuneralExternalMembershipCoverEntity cover) {
        return FuneralMembershipCoverDto.builder()
                .membershipId("EXTERNAL:" + cover.getId())
                .membershipNumber(cover.getSourceMembershipNo())
                .burialSocietyName(cover.getBurialSocietyName())
                .burialSocietyPartnerId(cover.getBurialSocietyPartnerId())
                .coverAmountCents(cover.getCoverAmountCents())
                .coverSource(COVER_SOURCE_EXTERNAL)
                .sourceTenantId(cover.getSourceTenantId())
                .sourceTenantName(cover.getSourceTenantName())
                .sourceMembershipId(cover.getSourceMembershipId())
                .sourceReference(cover.getSourceReference())
                .deceasedType("MAIN_MEMBER")
                .build();
    }

    private Map<String, FuneralMembershipCoverDto> resolveSelectedCovers(FuneralServiceEntity service, List<String> selectionIds) {
        Map<String, FuneralMembershipCoverDto> map = new HashMap<>();
        if (selectionIds == null) return map;

        List<String> externalIds = new ArrayList<>();
        for (String selectionId : selectionIds) {
            if (selectionId == null) continue;
            if (selectionId.startsWith("EXTERNAL:")) {
                externalIds.add(selectionId.substring("EXTERNAL:".length()));
            } else if (selectionId.startsWith("LOCAL:")) {
                FuneralMembershipCoverDto dto = resolveLocalCoverSelection(selectionId, service);
                map.put(selectionId, dto);
            }
        }
        if (!externalIds.isEmpty()) {
            externalMembershipCoverRepository.findByIdInAndStatus(externalIds, "ACTIVE")
                    .forEach(cover -> map.put("EXTERNAL:" + cover.getId(), toExternalCoverDto(cover)));
        }
        return map;
    }

    private FuneralMembershipCoverDto resolveLocalCoverSelection(String selectionId, FuneralServiceEntity service) {
        String[] parts = selectionId.split(":");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid local membership selection id: " + selectionId);
        }
        String membershipId = parts[1];
        String deceasedPartnerId = parts[2];
        String deceasedType = parts[3];
        Map<String, Object> membership = jdbcTemplate.queryForMap("""
                SELECT m.id, m.membership_no, m.plan_id, mp.name AS plan_name
                  FROM membership m
                  JOIN membership_plan mp ON mp.id = m.plan_id
                 WHERE m.id = ? AND m.status = 'ACTIVE'
                """, membershipId);
        Long payout = jdbcTemplate.query("""
                SELECT COALESCE(MAX(payout_amount_cents), 0)
                  FROM membership_plan_claim_payout
                 WHERE plan_id = ? AND claim_type = 'FUNERAL' AND active = 1
                   AND dependent_type IN (?, 'ANY')
                """, rs -> rs.next() ? rs.getLong(1) : 0L,
                membership.get("plan_id"), "MAIN_MEMBER".equals(deceasedType) ? "MAIN_MEMBER" : "DEPENDENT");
        return FuneralMembershipCoverDto.builder()
                .membershipId(selectionId)
                .sourceMembershipId(membershipId)
                .membershipNumber(String.valueOf(membership.get("membership_no")))
                .deceasedPartnerId(deceasedPartnerId)
                .deceasedType(deceasedType)
                .coverAmountCents(payout)
                .burialSocietyName(String.valueOf(membership.get("plan_name")))
                .coverSource(COVER_SOURCE_LOCAL)
                .build();
    }

    private List<FuneralInvoiceSplitDto> buildSplitsFromApprovedClaims(FuneralServiceEntity service) {
        List<FuneralClaimDto> claims = getClaims(service.getId());
        boolean hasPending = claims.stream().anyMatch(c -> List.of("DRAFT", "SUBMITTED").contains(defaultString(c.getStatus(), "")));
        if (hasPending) {
            throw new IllegalArgumentException("All funeral claims must be approved, partially approved, rejected, cancelled or paid before invoices are generated");
        }

        long remaining = defaultLong(service.getTotalAmountCents());
        List<FuneralInvoiceSplitDto> splits = new ArrayList<>();
        for (FuneralClaimDto claim : claims) {
            if (remaining <= 0) break;
            if (!List.of("APPROVED", "PARTIALLY_APPROVED", "PAID").contains(defaultString(claim.getStatus(), ""))) continue;
            long amount = Math.min(defaultLong(claim.getApprovedAmountCents()), remaining);
            if (amount <= 0) continue;
            String partnerId = resolveInvoicePartnerForClaim(claim);
            splits.add(FuneralInvoiceSplitDto.builder()
                    .entityName(COVER_SOURCE_EXTERNAL.equals(claim.getCoverSource()) ? defaultString(claim.getSourceTenantName(), "External Burial Society") : defaultString(claim.getMembershipNumber(), "Burial Society"))
                    .entityType("BURIAL_SOCIETY")
                    .partnerId(partnerId)
                    .amountCents(amount)
                    .description("Approved funeral claim " + claim.getClaimNo())
                    .membershipClaimId(claim.getMembershipClaimId())
                    .coverSource(claim.getCoverSource())
                    .sourceTenantId(claim.getSourceTenantId())
                    .build());
            remaining -= amount;
        }

        if (remaining > 0) {
            splits.add(FuneralInvoiceSplitDto.builder()
                    .entityName("Family Representative")
                    .entityType("FAMILY_REP")
                    .partnerId(service.getFamilyRepId())
                    .amountCents(remaining)
                    .description("Family payable balance")
                    .build());
        }
        return splits;
    }

    private String createInvoice(FuneralServiceEntity service, FuneralInvoiceSplitDto split) {
        String invoiceId = UUID.randomUUID().toString();
        String invoiceNo = generateInvoiceNo();
        jdbcTemplate.update("""
                INSERT INTO invoice
                (id, invoice_no, external_ref, partner_id, invoice_date, due_date, status, subtotal_cents, tax_cents,
                 discount_cents, total_cents, paid_cents, balance_cents, currency, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'ISSUED', ?, 0, 0, ?, 0, ?, 'ZAR', ?, CURRENT_TIMESTAMP)
                """,
                invoiceId,
                invoiceNo,
                "FUNERAL:" + service.getId(),
                split.getPartnerId(),
                LocalDate.now(),
                service.getFuneralDate(),
                split.getAmountCents(),
                split.getAmountCents(),
                split.getAmountCents(),
                split.getDescription());

        jdbcTemplate.update("""
                INSERT INTO invoice_line
                (id, invoice_id, description, quantity, unit_price_cents, discount_cents, tax_cents, subtotal_cents, total_cents, created_at)
                VALUES (?, ?, ?, 1.000, ?, 0, 0, ?, ?, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID().toString(), invoiceId,
                "Funeral service - " + service.getDeceasedName() + " - " + split.getEntityType(),
                split.getAmountCents(), split.getAmountCents(), split.getAmountCents());
        return invoiceId;
    }

    private FuneralClaimDto readClaimDto(String membershipClaimId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM membership_claim WHERE id = ?", membershipClaimId);
        Optional<FuneralServiceClaimEntity> linkOpt = funeralServiceClaimRepository.findByMembershipClaimId(membershipClaimId);
        FuneralServiceClaimEntity link = linkOpt.orElse(null);
        return FuneralClaimDto.builder()
                .funeralServiceClaimId(link == null ? null : link.getId())
                .membershipClaimId(String.valueOf(row.get("id")))
                .claimNo(String.valueOf(row.get("claim_no")))
                .membershipId(String.valueOf(row.get("membership_id")))
                .membershipNumber(resolveMembershipNo(String.valueOf(row.get("membership_id"))))
                .claimType(String.valueOf(row.get("claim_type")))
                .deceasedType(String.valueOf(row.get("deceased_type")))
                .deceasedPartnerId(String.valueOf(row.get("deceased_partner_id")))
                .claimantPartnerId(row.get("claimant_partner_id") == null ? null : String.valueOf(row.get("claimant_partner_id")))
                .dateOfDeath(asLocalDate(row.get("date_of_death")))
                .claimedAmountCents(asLong(row.get("claim_amount_cents")))
                .approvedAmountCents(resolveApprovedAmount(row))
                .status(String.valueOf(row.get("status")))
                .coverSource(link == null ? COVER_SOURCE_LOCAL : link.getCoverSource())
                .sourceTenantId(link == null ? null : link.getSourceTenantId())
                .sourceTenantName(link == null ? null : link.getSourceTenantName())
                .sourceMembershipId(link == null ? null : link.getSourceMembershipId())
                .sourceReference(link == null ? null : link.getSourceReference())
                .approvedAt(asLocalDateTime(row.get("approved_at")))
                .build();
    }

    private Long resolveApprovedAmount(Map<String, Object> row) {
        String status = String.valueOf(row.get("status"));
        if (!isApprovedStatus(status)) {
            return 0L;
        }
        Long approvedAmount = asLong(row.get("approved_amount_cents"));
        return approvedAmount == null ? asLong(row.get("claim_amount_cents")) : approvedAmount;
    }

    private String resolveInvoicePartnerForClaim(FuneralClaimDto claim) {
        Optional<FuneralServiceClaimEntity> link = funeralServiceClaimRepository.findByMembershipClaimId(claim.getMembershipClaimId());
        if (link.isPresent() && link.get().getBurialSocietyPartnerId() != null && !link.get().getBurialSocietyPartnerId().isBlank()) {
            return link.get().getBurialSocietyPartnerId();
        }
        // Fallback: invoice the membership owner for same-tenant records until group society partner mapping is added.
        return jdbcTemplate.queryForObject("SELECT member_id FROM membership WHERE id = ?", String.class, claim.getMembershipId());
    }

    private String createExternalMembershipPlaceholderIfRequired(FuneralMembershipCoverDto cover) {
        // membership_claim has a required FK to membership. For external cover, create or reuse a local placeholder
        // membership row against the local partner representing the external society/family payer.
        if (cover.getBurialSocietyPartnerId() == null || cover.getBurialSocietyPartnerId().isBlank()) {
            throw new IllegalArgumentException("External cover requires burialSocietyPartnerId so the local tenant has a partner to invoice and link");
        }
        String membershipNo = "EXT-" + cover.getSourceTenantId() + "-" + cover.getSourceMembershipId();
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM membership WHERE membership_no = ?", String.class, membershipNo);
        } catch (EmptyResultDataAccessException ignored) {
            String planId = ensureExternalFuneralPlan();
            String membershipId = UUID.randomUUID().toString();
            jdbcTemplate.update("""
                    INSERT INTO membership (id, member_id, membership_no, plan_id, start_date, status, created_at)
                    VALUES (?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)
                    """, membershipId, cover.getBurialSocietyPartnerId(), membershipNo, planId, LocalDate.now());
            return membershipId;
        }
    }

    private String ensureExternalFuneralPlan() {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM membership_plan WHERE plan_code = 'EXT-FUNERAL-COVER'", String.class);
        } catch (EmptyResultDataAccessException ignored) {
            String planId = UUID.randomUUID().toString();
            jdbcTemplate.update("""
                    INSERT INTO membership_plan (id, plan_code, name, description, premium_cents, currency, active, created_at)
                    VALUES (?, 'EXT-FUNERAL-COVER', 'External Funeral Cover', 'Placeholder plan for claims from external Mawa tenants', 0, 'ZAR', 1, CURRENT_TIMESTAMP)
                    """, planId);
            return planId;
        }
    }


    private void populateServiceRequestDefaults(FuneralServiceRequestDto request) {
        if (request == null) return;
        if ((request.getDeceasedName() == null || request.getDeceasedName().isBlank())
                && request.getMortuaryInventoryId() != null
                && !request.getMortuaryInventoryId().isBlank()) {
            mortuaryInventoryRepository.findById(request.getMortuaryInventoryId())
                    .ifPresent(inventory -> {
                        request.setDeceasedName(inventory.getDeceasedName());
                        if ((request.getDeceasedIdentityNumber() == null || request.getDeceasedIdentityNumber().isBlank())
                                && inventory.getIdentityNumber() != null) {
                            request.setDeceasedIdentityNumber(inventory.getIdentityNumber());
                        }
                        if ((request.getDeceasedPartnerId() == null || request.getDeceasedPartnerId().isBlank())
                                && inventory.getDeceasedPartnerId() != null) {
                            request.setDeceasedPartnerId(inventory.getDeceasedPartnerId());
                        }
                    });
        }
    }

    private String resolveDeceasedPartnerId(FuneralServiceRequestDto request) {
        if (request.getDeceasedPartnerId() != null && !request.getDeceasedPartnerId().isBlank()) return request.getDeceasedPartnerId();
        if (request.getDeceasedIdentityNumber() == null || request.getDeceasedIdentityNumber().isBlank()) return null;
        try {
            return jdbcTemplate.queryForObject("SELECT partner FROM partner_identity WHERE value = ? LIMIT 1", String.class, request.getDeceasedIdentityNumber());
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private void updateFuneralServiceClaimStatus(String membershipClaimId) {
        funeralServiceClaimRepository.findByMembershipClaimId(membershipClaimId).ifPresent(link -> {
            FuneralServiceEntity service = getFuneralServiceOrThrow(link.getFuneralServiceId());
            List<FuneralClaimDto> claims = getClaims(service.getId());
            boolean anyPending = claims.stream().anyMatch(c -> List.of("DRAFT", "SUBMITTED").contains(defaultString(c.getStatus(), "")));
            service.setStatus(anyPending ? "CLAIMS_INITIATED" : "CLAIMS_RESOLVED");
            funeralServiceRepository.save(service);
        });
    }

    private FuneralServiceRequestResponseDto toServiceResponse(FuneralServiceEntity entity) {
        return FuneralServiceRequestResponseDto.builder()
                .id(entity.getId())
                .mortuaryInventoryId(entity.getMortuaryInventoryId())
                .deceasedName(entity.getDeceasedName())
                .deceasedIdentityNumber(entity.getDeceasedIdentityNumber())
                .deceasedPartnerId(entity.getDeceasedPartnerId())
                .packageId(entity.getPackageId())
                .familyRepId(entity.getFamilyRepId())
                .funeralDate(entity.getFuneralDate())
                .funeralArea(entity.getFuneralArea())
                .totalAmountCents(entity.getTotalAmountCents())
                .status(entity.getStatus())
                .build();
    }

    private FuneralPickupRequestEntity getPickupRequestOrThrow(String id) {
        return pickupRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pickup request not found: " + id));
    }

    private FuneralServiceEntity getFuneralServiceOrThrow(String id) {
        return funeralServiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Funeral service request not found: " + id));
    }

    private long calculateExtrasTotal(List<FuneralExtraDto> extras) {
        if (extras == null) return 0L;
        return extras.stream().mapToLong(e -> defaultLong(e.getAmountCents())).sum();
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize extras/inclusions", exception);
        }
    }

    private String generateTagNumber(LocalDateTime time) {
        return "MORT-" + time.toLocalDate().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateClaimNo() {
        return "CLM-FUN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String generateInvoiceNo() {
        return "INV-FUN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String resolveMembershipNo(String membershipId) {
        try {
            return jdbcTemplate.queryForObject("SELECT membership_no FROM membership WHERE id = ?", String.class, membershipId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isApprovedStatus(String status) {
        return List.of("APPROVED", "PARTIALLY_APPROVED", "PAID").contains(defaultString(status, ""));
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(fieldName + " is required");
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private long asLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private LocalDate asLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        return LocalDateTime.parse(String.valueOf(value));
    }
}
