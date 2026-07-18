package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.v2.funeral.*;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.FuneralPackageCreateRequestDto;
import za.co.mawa.bes.dto.v2.FuneralPackageItemRequestDto;
import za.co.mawa.bes.dto.v2.FuneralPackageUpdateRequestDto;
import za.co.mawa.bes.entity.InvoiceEntity;
import za.co.mawa.bes.entity.AttachmentEntity;
import za.co.mawa.bes.repository.AttachmentRepository;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.repository.InvoiceRepository;
import za.co.mawa.bes.repository.v2.*;
import za.co.mawa.bes.service.v2.claim.ClaimFormGenerationService;
import za.co.mawa.bes.service.NumberRangeService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.enums.ApprovalType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuneralManagementService {

    private static final String COVER_SOURCE_LOCAL = "LOCAL_TENANT";
    private static final String COVER_SOURCE_EXTERNAL = "EXTERNAL_TENANT";
    private static final String SOURCE_MODE_LOCAL_ONLY = "LOCAL_ONLY";
    private static final String SOURCE_MODE_EXTERNAL_ONLY = "EXTERNAL_ONLY";
    private static final String SOURCE_MODE_LOCAL_AND_EXTERNAL = "LOCAL_AND_EXTERNAL";
    private static final String INTEGRATION_CONFIG_ID = "DEFAULT";
    private static final String FUNERAL_SERVICE_SETTING = "FUNERAL-SERVICE";
    private static final String MAX_SELECTED_COVERS_ATTRIBUTE = "MAX-SELECTED-COVERS";

    private final FuneralPickupRequestRepository pickupRequestRepository;
    private final FuneralMortuaryInventoryRepository mortuaryInventoryRepository;
    private final FuneralPackageRepository funeralPackageRepository;
    private final FuneralPackageItemRepository funeralPackageItemRepository;
    private final za.co.mawa.bes.repository.ProductRepository productRepository;
    private final FuneralServiceRepository funeralServiceRepository;
    private final FuneralServiceInvoiceRepository funeralServiceInvoiceRepository;
    private final InvoiceRepository invoiceRepository;
    private final AttachmentRepository attachmentRepository;
    private final FuneralServiceClaimRepository funeralServiceClaimRepository;
    private final FuneralExternalMembershipCoverRepository externalMembershipCoverRepository;
    private final FuneralTenantIntegrationConfigRepository tenantIntegrationConfigRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ClaimFormGenerationService claimFormGenerationService;
    private final ApprovalService approvalService;
    private final FuneralClaimSettlementService funeralClaimSettlementService;
    private final NumberAllocationService numberAllocationService;
    private final NumberRangeService numberRangeService;
    private final SettingService settingService;
    private final TenantAdminService tenantAdminService;

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
            return attachPackageItems(funeralPackageRepository.findByActiveTrue());
        }
        return attachPackageItems(funeralPackageRepository.findAll());
    }

    public FuneralPackageEntity getPackage(String id) {
        return attachPackageItems(getFuneralPackageOrThrow(id));
    }

    @Transactional
    public FuneralPackageEntity createPackage(FuneralPackageCreateRequestDto request) {
        validateRequired(request.getName(), "name");
        FuneralPackageEntity entity = new FuneralPackageEntity();
        entity.setName(request.getName().trim());
        entity.setInclusionsJson("[]");
        entity.setActive(request.getActive() == null || request.getActive());
        entity = funeralPackageRepository.save(entity);
        replacePackageItems(entity, request.getProducts());
        return attachPackageItems(entity);
    }

    @Transactional
    public FuneralPackageEntity updatePackage(String id, FuneralPackageUpdateRequestDto request) {
        validateRequired(id, "id");
        validateRequired(request.getName(), "name");
        FuneralPackageEntity entity = getFuneralPackageOrThrow(id);
        entity.setName(request.getName().trim());
        entity.setInclusionsJson("[]");
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        entity = funeralPackageRepository.save(entity);
        replacePackageItems(entity, request.getProducts());
        return attachPackageItems(entity);
    }

    private List<FuneralPackageEntity> attachPackageItems(List<FuneralPackageEntity> packages) {
        packages.forEach(this::attachPackageItems);
        return packages;
    }

    private FuneralPackageEntity attachPackageItems(FuneralPackageEntity entity) {
        entity.setProducts(funeralPackageItemRepository.findByFuneralPackageIdOrderByProductDescriptionAsc(entity.getId()));
        return entity;
    }

    private void replacePackageItems(FuneralPackageEntity funeralPackage, List<FuneralPackageItemRequestDto> products) {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("A funeral package must contain at least one product");
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        funeralPackageItemRepository.deleteByFuneralPackageId(funeralPackage.getId());
        long total = 0L;
        for (FuneralPackageItemRequestDto item : products) {
            if (item.getProductId() == null || item.getProductId().isBlank()) throw new IllegalArgumentException("productId is required");
            if (!seen.add(item.getProductId())) throw new IllegalArgumentException("A product may only appear once in a funeral package");
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity <= 0) throw new IllegalArgumentException("Product quantity must be greater than zero");
            long unitPrice = item.getUnitPriceCents() == null ? 0L : item.getUnitPriceCents();
            if (unitPrice < 0) throw new IllegalArgumentException("Product unit price cannot be negative");
            za.co.mawa.bes.entity.ProductEntity product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));
            long lineTotal = Math.multiplyExact(unitPrice, quantity);
            total = Math.addExact(total, lineTotal);
            funeralPackageItemRepository.save(FuneralPackageItemEntity.builder()
                    .funeralPackageId(funeralPackage.getId()).productId(product.getId())
                    .productCode(product.getCode()).productDescription(product.getDescription())
                    .quantity(quantity).unitPriceCents(unitPrice).lineTotalCents(lineTotal).build());
        }
        funeralPackage.setBasePriceCents(total);
        funeralPackageRepository.save(funeralPackage);
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

    public List<FuneralMembershipCoverDto> checkMembership(String identityNumber) {
        validateRequired(identityNumber, "identityNumber");
        FuneralTenantIntegrationConfigDto config = getTenantIntegrationConfiguration();
        List<FuneralMembershipCoverDto> result = new ArrayList<>();
        if (includesLocalSource(config)) {
            result.addAll(findLocalMembershipCover(identityNumber));
        }
        if (includesExternalSource(config)
                && Boolean.TRUE.equals(config.getActive())
                && Boolean.TRUE.equals(config.getMembershipLookupEnabled())) {
            result.addAll(findExternalMembershipCover(identityNumber, config));
        }
        return result;
    }

    public FuneralTenantIntegrationConfigDto getTenantIntegrationConfiguration() {
        return tenantIntegrationConfigRepository.findById(INTEGRATION_CONFIG_ID)
                .map(this::toTenantIntegrationConfigDto)
                .orElseGet(() -> FuneralTenantIntegrationConfigDto.builder()
                        .membershipSourceMode(SOURCE_MODE_LOCAL_ONLY)
                        .membershipLookupEnabled(true)
                        .claimCreationEnabled(true)
                        .claimStatusSyncEnabled(true)
                        .active(true)
                        .build());
    }

    @Transactional
    public FuneralTenantIntegrationConfigDto updateTenantIntegrationConfiguration(FuneralTenantIntegrationConfigDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Configuration is required");
        }
        String sourceMode = normalizeSourceMode(request.getMembershipSourceMode());
        boolean externalRequired = SOURCE_MODE_EXTERNAL_ONLY.equals(sourceMode)
                || SOURCE_MODE_LOCAL_AND_EXTERNAL.equals(sourceMode);

        String externalTenantId = trimToNull(request.getExternalTenantId());
        String externalTenantName = trimToNull(request.getExternalTenantName());
        String externalPartnerId = trimToNull(request.getExternalTenantPartnerId());

        if (externalRequired) {
            validateRequired(externalTenantId, "externalTenantId");
            validateRequired(externalPartnerId, "externalTenantPartnerId");
            if (externalTenantId.equals(TenantContext.getCurrentTenant())) {
                throw new IllegalArgumentException("External tenant must be different from the current tenant");
            }
            final String selectedExternalTenantId = externalTenantId;
            FuneralTenantOptionDto selectedTenant = getAvailableTenantOptions().stream()
                    .filter(option -> selectedExternalTenantId.equals(option.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "External tenant is not an active MAWA tenant: " + selectedExternalTenantId));
            if (!schemaExists(externalTenantId)) {
                throw new IllegalArgumentException("External tenant schema is not available: " + externalTenantId);
            }
            requireApprovedTrust(externalTenantId, "allow_membership_lookup");
            Integer localPartnerCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM partner WHERE id = ?", Integer.class, externalPartnerId);
            if (localPartnerCount == null || localPartnerCount == 0) {
                throw new IllegalArgumentException("The mapped local partner does not exist: " + externalPartnerId);
            }
            externalTenantName = defaultString(selectedTenant.getName(), externalTenantId);
        } else {
            externalTenantId = null;
            externalTenantName = null;
            externalPartnerId = null;
        }

        FuneralTenantIntegrationConfigEntity entity = tenantIntegrationConfigRepository
                .findById(INTEGRATION_CONFIG_ID)
                .orElseGet(() -> FuneralTenantIntegrationConfigEntity.builder().id(INTEGRATION_CONFIG_ID).build());
        boolean membershipLookupEnabled = defaultBoolean(request.getMembershipLookupEnabled(), true);
        boolean claimCreationEnabled = defaultBoolean(request.getClaimCreationEnabled(), true);
        boolean claimStatusSyncEnabled = defaultBoolean(request.getClaimStatusSyncEnabled(), true);
        if (externalRequired && claimCreationEnabled && !claimStatusSyncEnabled) {
            throw new IllegalArgumentException(
                    "Claim status synchronisation must be enabled when external claim creation is enabled");
        }

        entity.setMembershipSourceMode(sourceMode);
        entity.setExternalTenantId(externalTenantId);
        entity.setExternalTenantName(externalTenantName);
        entity.setExternalTenantPartnerId(externalPartnerId);
        entity.setMembershipLookupEnabled(membershipLookupEnabled);
        entity.setClaimCreationEnabled(claimCreationEnabled);
        entity.setClaimStatusSyncEnabled(claimStatusSyncEnabled);
        entity.setActive(defaultBoolean(request.getActive(), true));
        return toTenantIntegrationConfigDto(tenantIntegrationConfigRepository.save(entity));
    }

    public List<FuneralTenantOptionDto> getAvailableTenantOptions() {
        String currentTenant = TenantContext.getCurrentTenant();
        return tenantAdminService.getAll().stream()
                .filter(tenant -> tenant != null && StringUtils.hasText(tenant.getId()))
                .filter(tenant -> !tenant.getId().equals(currentTenant))
                .filter(tenant -> !StringUtils.hasText(tenant.getStatus())
                        || "ACTIVE".equalsIgnoreCase(tenant.getStatus()))
                .map(tenant -> FuneralTenantOptionDto.builder()
                        .id(tenant.getId())
                        .name(tenant.getName())
                        .host(tenant.getHost())
                        .status(tenant.getStatus() == null ? null : tenant.getStatus().toString())
                        .build())
                .sorted(Comparator.comparing(
                        (FuneralTenantOptionDto option) -> defaultString(option.getName(), option.getId()),
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<FuneralServiceRequestResponseDto> getServiceRequests(String query, String status) {
        String normalizedQuery = query == null ? null : query.trim();
        String normalizedStatus = status == null ? null : status.trim();
        return funeralServiceRepository.search(normalizedQuery, normalizedStatus)
                .stream()
                .map(this::toServiceResponse)
                .collect(Collectors.toList());
    }

    public List<FuneralPaymentSummaryDto> getFuneralPayments() {
        return funeralServiceInvoiceRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        FuneralServiceInvoiceEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(link -> {
                    InvoiceEntity invoice = invoiceRepository.findById(link.getInvoiceId()).orElse(null);
                    FuneralServiceEntity service = funeralServiceRepository
                            .findById(link.getFuneralServiceId())
                            .orElse(null);

                    return FuneralPaymentSummaryDto.builder()
                            .funeralServiceInvoiceId(link.getId())
                            .funeralServiceId(link.getFuneralServiceId())
                            .serviceRequestNo(service == null ? null : service.getServiceRequestNo())
                            .deceasedName(service == null ? null : service.getDeceasedName())
                            .invoiceId(link.getInvoiceId())
                            .invoiceNo(invoice == null ? null : invoice.getInvoiceNo())
                            .entityType(link.getEntityType())
                            .partnerId(link.getPartnerId())
                            .allocatedAmountCents(defaultLong(link.getAmountCents()))
                            .invoiceTotalCents(invoice == null ? 0L : defaultLong(invoice.getTotalCents()))
                            .paidCents(invoice == null ? 0L : defaultLong(invoice.getPaidCents()))
                            .balanceCents(invoice == null ? 0L : defaultLong(invoice.getBalanceCents()))
                            .status(invoice == null ? "UNKNOWN" : invoice.getStatus())
                            .invoiceDate(invoice == null ? null : invoice.getInvoiceDate())
                            .build();
                })
                .toList();
    }

    public FuneralServiceConfigurationDto getServiceConfiguration() {
        int maxSelectableCovers = getMaxSelectableCovers();
        return FuneralServiceConfigurationDto.builder()
                .maxSelectableCovers(maxSelectableCovers)
                .coverSelectionLimitEnabled(maxSelectableCovers > 0)
                .build();
    }

    @Transactional
    public FuneralServiceConfigurationDto updateServiceConfiguration(FuneralServiceConfigurationDto request) {
        int maxSelectableCovers = normalizeMaxSelectableCovers(request == null ? null : request.getMaxSelectableCovers());
        settingService.upsertSetting(MAX_SELECTED_COVERS_ATTRIBUTE, FUNERAL_SERVICE_SETTING, String.valueOf(maxSelectableCovers));
        return getServiceConfiguration();
    }

    public FuneralServiceRequestResponseDto getServiceRequest(String id) {
        return toServiceResponse(getFuneralServiceOrThrow(id));
    }

    @Transactional
    public FuneralServiceRequestResponseDto createServiceRequest(FuneralServiceRequestDto request) {
        populateServiceRequestDefaults(request);
        validateRequired(request.getDeceasedName(), "deceasedName");
        validateRequired(request.getFamilyRepId(), "familyRepId");

        FuneralPackageEntity packageEntity = null;
        if (request.getPackageId() != null && !request.getPackageId().isBlank()) {
            packageEntity = funeralPackageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> new IllegalArgumentException("Funeral package not found: " + request.getPackageId()));
        }

        FuneralServiceEntity entity = new FuneralServiceEntity();
        entity.setServiceRequestNo(generateFuneralServiceRequestNo());
        entity.setMortuaryInventoryId(request.getMortuaryInventoryId());
        entity.setDeceasedName(request.getDeceasedName());
        entity.setDeceasedIdentityNumber(request.getDeceasedIdentityNumber());
        entity.setDeceasedPartnerId(resolveDeceasedPartnerId(request));
        entity.setPackageId(request.getPackageId());
        entity.setFamilyRepId(request.getFamilyRepId());
        entity.setFuneralDate(request.getFuneralDate());
        entity.setFuneralArea(request.getFuneralArea());
        entity.setDeathCertificateNo(request.getDeathCertificateNo());
        entity.setCauseOfDeath(request.getCauseOfDeath());
        entity.setExtrasJson(toJson(request.getExtras()));
        entity.setTotalAmountCents((packageEntity == null ? 0L : defaultLong(packageEntity.getBasePriceCents())) + calculateExtrasTotal(request.getExtras()));
        entity.setStatus(packageEntity == null ? "COVER_IDENTIFIED" : "ARRANGEMENT_CREATED");
        return toServiceResponse(funeralServiceRepository.save(entity));
    }

    @Transactional
    public FuneralServiceRequestResponseDto updateServiceRequestPackage(String funeralServiceId, FuneralServiceRequestDto request) {
        validateRequired(funeralServiceId, "funeralServiceId");
        validateRequired(request.getPackageId(), "packageId");

        FuneralServiceEntity service = getFuneralServiceOrThrow(funeralServiceId);
        FuneralPackageEntity packageEntity = funeralPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Funeral package not found: " + request.getPackageId()));

        service.setPackageId(request.getPackageId());
        service.setExtrasJson(toJson(request.getExtras()));
        service.setTotalAmountCents(defaultLong(packageEntity.getBasePriceCents()) + calculateExtrasTotal(request.getExtras()));
        if (request.getFuneralDate() != null) service.setFuneralDate(request.getFuneralDate());
        if (request.getFuneralArea() != null && !request.getFuneralArea().isBlank()) service.setFuneralArea(request.getFuneralArea());
        if (request.getDeathCertificateNo() != null && !request.getDeathCertificateNo().isBlank()) service.setDeathCertificateNo(request.getDeathCertificateNo());
        if (request.getCauseOfDeath() != null && !request.getCauseOfDeath().isBlank()) service.setCauseOfDeath(request.getCauseOfDeath());
        if (!"INVOICED".equalsIgnoreCase(defaultString(service.getStatus(), ""))) {
            service.setStatus("ARRANGEMENT_CREATED");
        }
        return toServiceResponse(funeralServiceRepository.save(service));
    }

    @Transactional
    public List<FuneralClaimDto> initiateClaims(String funeralServiceId, InitiateFuneralClaimsDto request) {
        FuneralServiceEntity service = getFuneralServiceOrThrow(funeralServiceId);
        List<String> selectedMemberships = request.getMemberships();
        if (selectedMemberships == null || selectedMemberships.isEmpty()) {
            throw new IllegalArgumentException("At least one membership selection is required");
        }
        validateSelectedCoverLimit(selectedMemberships);
        Map<String, FuneralMembershipCoverDto> coverMap = resolveSelectedCovers(service, selectedMemberships);
        if (coverMap.isEmpty()) {
            throw new IllegalArgumentException("Selected membership cover could not be resolved. Please re-check membership cover and select again.");
        }
        boolean hasLocalCover = coverMap.values().stream()
                .anyMatch(cover -> COVER_SOURCE_LOCAL.equals(cover.getCoverSource()));
        if (hasLocalCover && !StringUtils.hasText(service.getDeceasedPartnerId())) {
            throw new IllegalArgumentException("Funeral service must have deceasedPartnerId before a local membership claim can be created");
        }

        String claimType = request.getEffectiveClaimType(selectedMemberships.size());
        long arrangementTotal = defaultLong(service.getTotalAmountCents());
        long remaining = arrangementTotal > 0
                ? arrangementTotal
                : coverMap.values().stream().mapToLong(cover -> defaultLong(cover.amountForClaimType(claimType))).sum();
        List<FuneralClaimDto> response = new ArrayList<>();

        for (String selectionId : selectedMemberships) {
            FuneralMembershipCoverDto cover = coverMap.get(selectionId);
            if (cover == null || remaining <= 0) continue;
            long coverAmount = defaultLong(cover.amountForClaimType(claimType));
            long claimAmount = arrangementTotal > 0 ? Math.min(coverAmount, remaining) : coverAmount;
            if (claimAmount <= 0) continue;

            String membershipClaimId = UUID.randomUUID().toString();
            boolean externalClaim = COVER_SOURCE_EXTERNAL.equals(cover.getCoverSource());
            String claimTenantId = externalClaim ? cover.getSourceTenantId() : TenantContext.getCurrentTenant();
            if (externalClaim) {
                ensureExternalClaimCreationAllowed(claimTenantId);
            }
            String claimNo = externalClaim
                    ? generateExternalMembershipClaimNo(claimTenantId)
                    : generateMembershipClaimNo();
            String membershipId = cover.getSourceMembershipId();
            String claimTable = externalClaim
                    ? qualifiedTable(claimTenantId, "membership_claim")
                    : "membership_claim";

            jdbcTemplate.update("""
                    INSERT INTO %s
                    (id, claim_no, membership_id, claim_type, deceased_type, deceased_partner_id, date_of_death,
                     claim_date, cause_of_death, death_certificate_no, claimant_partner_id, claim_amount_cents,
                     funeral_service_id, funeral_provider_tenant_id, status, notes, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, CURRENT_TIMESTAMP)
                    """.formatted(claimTable),
                    membershipClaimId,
                    claimNo,
                    membershipId,
                    claimType,
                    defaultString(cover.getDeceasedType(), "MAIN_MEMBER"),
                    defaultString(cover.getDeceasedPartnerId(), service.getDeceasedPartnerId()),
                    service.getFuneralDate() == null ? LocalDate.now() : service.getFuneralDate(),
                    LocalDate.now(),
                    defaultString(request.getCauseOfDeath(), service.getCauseOfDeath()),
                    defaultString(request.getDeathCertificateNo(), service.getDeathCertificateNo()),
                    service.getFamilyRepId(),
                    claimAmount,
                    service.getId(),
                    TenantContext.getCurrentTenant(),
                    buildClaimNotes(service, request.getNotes(), externalClaim));

            FuneralServiceClaimEntity link = new FuneralServiceClaimEntity();
            link.setFuneralServiceId(service.getId());
            link.setMembershipClaimId(membershipClaimId);
            link.setClaimStorageScope(externalClaim ? "EXTERNAL" : "LOCAL");
            link.setClaimOwnerTenantId(externalClaim ? claimTenantId : TenantContext.getCurrentTenant());
            link.setCoverSource(cover.getCoverSource());
            link.setSourceTenantId(cover.getSourceTenantId());
            link.setSourceTenantName(cover.getSourceTenantName());
            link.setSourceMembershipId(cover.getSourceMembershipId());
            link.setSourceReference(cover.getSourceReference());
            link.setBurialSocietyPartnerId(cover.getBurialSocietyPartnerId());
            funeralServiceClaimRepository.save(link);
            prepareFuneralClaimForm(service, membershipClaimId, claimNo, claimType, claimAmount);

            response.add(readClaimDto(membershipClaimId));
            remaining -= claimAmount;
        }

        String grocerySelection = trimToNull(request.getGroceryCoverSelectionId());
        if (grocerySelection == null) {
            grocerySelection = selectedMemberships.get(0);
        }
        FuneralMembershipCoverDto groceryCover = coverMap.get(grocerySelection);
        if (groceryCover == null) {
            throw new IllegalArgumentException("Select which funeral cover must fund the grocery claim");
        }
        createGroceryClaimForFuneral(service, groceryCover, request);

        service.setStatus("CLAIMS_INITIATED");
        funeralServiceRepository.save(service);
        return response;
    }


    @Transactional
    public FuneralClaimDto submitClaimForApproval(String membershipClaimId, String userId) {
        Optional<FuneralServiceClaimEntity> link = funeralServiceClaimRepository.findByMembershipClaimId(membershipClaimId);
        if (link.isPresent() && isExternalClaimStorage(link.get())) {
            throw new IllegalArgumentException(
                    "External claims must be reviewed and submitted from the source membership tenant");
        }

        Map<String, Object> claim = jdbcTemplate.queryForMap("SELECT id, claim_no, claim_type, claim_amount_cents, status FROM membership_claim WHERE id = ?", membershipClaimId);
        String status = String.valueOf(claim.get("status"));
        if (attachmentRepository.findByObjectId(membershipClaimId).isEmpty()) {
            throw new IllegalArgumentException("Attach the signed claim form and supporting claim documentation before submitting for approval");
        }
        if (!"DRAFT".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Only DRAFT claims can be submitted for approval. Current status: " + status);
        }

        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(ApprovalType.CLAIM);
        request.setReferenceId(membershipClaimId);
        request.setReferenceNo(String.valueOf(claim.get("claim_no")));
        request.setTitle("Membership claim " + claim.get("claim_no"));
        request.setDescription("Funeral arrangement claim submitted for approval");
        request.setRequesterId(userId);
        try {
            request.setPayloadJson(objectMapper.writeValueAsString(claim));
        } catch (JsonProcessingException ignored) {
            request.setPayloadJson("{}");
        }
        approvalService.submitForApproval(request);
        updateFuneralServiceClaimStatus(membershipClaimId);
        return readClaimDto(membershipClaimId);
    }

    public List<FuneralClaimDto> getClaims(String funeralServiceId) {
        List<FuneralClaimDto> claims = funeralServiceClaimRepository.findByFuneralServiceId(funeralServiceId)
                .stream()
                .map(link -> readClaimDto(link.getMembershipClaimId()))
                .collect(Collectors.toList());
        refreshFuneralServiceStatus(funeralServiceId, claims);
        return claims;
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

        Optional<FuneralServiceClaimEntity> link = funeralServiceClaimRepository.findByMembershipClaimId(membershipClaimId);
        if (link.isPresent() && isExternalClaimStorage(link.get())) {
            throw new IllegalArgumentException(
                    "External claims must be approved, rejected or cancelled in the source membership tenant");
        }
        String claimTable = "membership_claim";

        Long claimAmount = jdbcTemplate.queryForObject(
                "SELECT claim_amount_cents FROM " + claimTable + " WHERE id = ?",
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
                UPDATE %s
                   SET status = ?,
                       approved_amount_cents = ?,
                       rejection_reason = ?,
                       approved_at = CASE WHEN ? IN ('APPROVED','PARTIALLY_APPROVED','PAID','REJECTED','CANCELLED') THEN CURRENT_TIMESTAMP ELSE approved_at END,
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                """.formatted(claimTable), status, approvedAmount, request.getDecisionNotes(), status, membershipClaimId);

        updateFuneralServiceClaimStatus(membershipClaimId);
        if (List.of("APPROVED","PARTIALLY_APPROVED").contains(status)) {
            funeralClaimSettlementService.settleApprovedClaim(membershipClaimId, "SYSTEM");
        }
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
            List<String> selectedMemberships = request.getMemberships();
            validateSelectedCoverLimit(selectedMemberships);
            String claimType = request.getEffectiveClaimType(selectedMemberships.size());
            Map<String, FuneralMembershipCoverDto> covers = resolveSelectedCovers(null, selectedMemberships);
            for (String selectionId : selectedMemberships) {
                FuneralMembershipCoverDto cover = covers.get(selectionId);
                if (cover == null || remaining <= 0) continue;
                long amount = Math.min(defaultLong(cover.amountForClaimType(claimType)), remaining);
                if (amount <= 0) continue;
                splits.add(FuneralInvoiceSplitDto.builder()
                        .entityName(cover.getBurialSocietyName())
                        .entityType("BURIAL_SOCIETY")
                        .partnerId(cover.getBurialSocietyPartnerId())
                        .amountCents(amount)
                        .description("Estimated " + claimType.toLowerCase() + " cover pending claim approval")
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
            Map<String,String> identity = resolveInvoiceIdentity(service, split);
            link.setMembershipHolderName(identity.get("holderName"));
            link.setMembershipHolderIdentity(identity.get("holderIdentity"));
            link.setDeceasedName(defaultString(service.getDeceasedName(), identity.get("deceasedName")));
            link.setDeceasedIdentity(defaultString(service.getDeceasedIdentityNumber(), identity.get("deceasedIdentity")));
            link.setProviderTenantId(TenantContext.getCurrentTenant());
            link.setCoverTenantId(defaultString(split.getSourceTenantId(), TenantContext.getCurrentTenant()));
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
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'FUNERAL' THEN pay.payout_amount_cents END), 0) AS funeral_amount_cents,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'COMBINATION' THEN pay.payout_amount_cents END), 0) AS combination_amount_cents,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'FUNERAL' THEN pay.payout_amount_cents END), 0) AS cover_amount_cents,
                       COALESCE(mp.name, 'Burial Society') AS burial_society_name,
                       NULL AS burial_society_partner_id
                  FROM partner_identity pi
                  JOIN partner p ON p.id = pi.partner
                  JOIN membership m ON m.member_id = p.id
                  JOIN membership_plan mp ON mp.id = m.plan_id
             LEFT JOIN membership_plan_claim_payout pay ON pay.plan_id = m.plan_id
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
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'FUNERAL' THEN pay.payout_amount_cents END), 0) AS funeral_amount_cents,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'COMBINATION' THEN pay.payout_amount_cents END), 0) AS combination_amount_cents,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'FUNERAL' THEN pay.payout_amount_cents END), 0) AS cover_amount_cents,
                       COALESCE(mp.name, 'Burial Society') AS burial_society_name,
                       NULL AS burial_society_partner_id
                  FROM partner_identity pi
                  JOIN partner p ON p.id = pi.partner
                  JOIN membership_dependent md ON md.dependent_partner_id = p.id AND md.active = 1
                  JOIN membership m ON m.id = md.membership_id
                  JOIN membership_plan mp ON mp.id = m.plan_id
             LEFT JOIN membership_plan_claim_payout pay ON pay.plan_id = m.plan_id
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
                .funeralAmountCents(rs.getLong("funeral_amount_cents"))
                .combinationAmountCents(rs.getLong("combination_amount_cents"))
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
                .funeralAmountCents(rs.getLong("funeral_amount_cents"))
                .combinationAmountCents(rs.getLong("combination_amount_cents"))
                .burialSocietyName(rs.getString("burial_society_name"))
                .burialSocietyPartnerId(rs.getString("burial_society_partner_id"))
                .coverSource(COVER_SOURCE_LOCAL)
                .build(), identityNumber));
        return covers;
    }

    private List<FuneralMembershipCoverDto> findExternalMembershipCover(
            String identityNumber,
            FuneralTenantIntegrationConfigDto config
    ) {
        String tenantId = requireConfiguredExternalTenant(config);
        requireApprovedTrust(tenantId, "allow_membership_lookup");
        String tenantName = defaultString(config.getExternalTenantName(), tenantId);
        String localPartnerId = config.getExternalTenantPartnerId();
        String partnerIdentity = qualifiedTable(tenantId, "partner_identity");
        String partner = qualifiedTable(tenantId, "partner");
        String membership = qualifiedTable(tenantId, "membership");
        String membershipPlan = qualifiedTable(tenantId, "membership_plan");
        String payout = qualifiedTable(tenantId, "membership_plan_claim_payout");
        String dependent = qualifiedTable(tenantId, "membership_dependent");

        String mainMemberSql = """
                SELECT m.id AS membership_id,
                       m.membership_no AS membership_no,
                       p.id AS deceased_partner_id,
                       'MAIN_MEMBER' AS deceased_type,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'FUNERAL' THEN pay.payout_amount_cents END), 0) AS funeral_amount_cents,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'COMBINATION' THEN pay.payout_amount_cents END), 0) AS combination_amount_cents,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'FUNERAL' THEN pay.payout_amount_cents END), 0) AS cover_amount_cents,
                       COALESCE(mp.name, 'Burial Society') AS burial_society_name
                  FROM %s pi
                  JOIN %s p ON p.id = pi.partner
                  JOIN %s m ON m.member_id = p.id
                  JOIN %s mp ON mp.id = m.plan_id
             LEFT JOIN %s pay ON pay.plan_id = m.plan_id
                       AND pay.active = 1
                       AND pay.dependent_type IN ('MAIN_MEMBER', 'ANY')
                 WHERE pi.value = ?
                   AND m.status = 'ACTIVE'
              GROUP BY m.id, m.membership_no, p.id, mp.name
                """.formatted(partnerIdentity, partner, membership, membershipPlan, payout);

        String dependentSql = """
                SELECT m.id AS membership_id,
                       m.membership_no AS membership_no,
                       p.id AS deceased_partner_id,
                       'DEPENDENT' AS deceased_type,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'FUNERAL' THEN pay.payout_amount_cents END), 0) AS funeral_amount_cents,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'COMBINATION' THEN pay.payout_amount_cents END), 0) AS combination_amount_cents,
                       COALESCE(MAX(CASE WHEN pay.claim_type = 'FUNERAL' THEN pay.payout_amount_cents END), 0) AS cover_amount_cents,
                       COALESCE(mp.name, 'Burial Society') AS burial_society_name
                  FROM %s pi
                  JOIN %s p ON p.id = pi.partner
                  JOIN %s md ON md.dependent_partner_id = p.id AND md.active = 1
                  JOIN %s m ON m.id = md.membership_id
                  JOIN %s mp ON mp.id = m.plan_id
             LEFT JOIN %s pay ON pay.plan_id = m.plan_id
                       AND pay.active = 1
                       AND pay.dependent_type IN (md.relationship, 'DEPENDENT', 'ANY')
                 WHERE pi.value = ?
                   AND m.status = 'ACTIVE'
              GROUP BY m.id, m.membership_no, p.id, mp.name
                """.formatted(partnerIdentity, partner, dependent, membership, membershipPlan, payout);

        List<FuneralMembershipCoverDto> covers = new ArrayList<>();
        covers.addAll(jdbcTemplate.query(mainMemberSql, (rs, rowNum) -> toLiveExternalCover(
                tenantId,
                tenantName,
                localPartnerId,
                rs.getString("membership_id"),
                rs.getString("membership_no"),
                rs.getString("deceased_partner_id"),
                rs.getString("deceased_type"),
                rs.getString("burial_society_name"),
                rs.getLong("funeral_amount_cents"),
                rs.getLong("combination_amount_cents"),
                rs.getLong("cover_amount_cents")
        ), identityNumber));
        covers.addAll(jdbcTemplate.query(dependentSql, (rs, rowNum) -> toLiveExternalCover(
                tenantId,
                tenantName,
                localPartnerId,
                rs.getString("membership_id"),
                rs.getString("membership_no"),
                rs.getString("deceased_partner_id"),
                rs.getString("deceased_type"),
                rs.getString("burial_society_name"),
                rs.getLong("funeral_amount_cents"),
                rs.getLong("combination_amount_cents"),
                rs.getLong("cover_amount_cents")
        ), identityNumber));
        return covers;
    }

    private FuneralMembershipCoverDto toLiveExternalCover(
            String tenantId,
            String tenantName,
            String localPartnerId,
            String membershipId,
            String membershipNumber,
            String deceasedPartnerId,
            String deceasedType,
            String burialSocietyName,
            Long funeralAmount,
            Long combinationAmount,
            Long coverAmount
    ) {
        String selectionId = String.join(":", "EXTERNAL", tenantId, membershipId, deceasedPartnerId, deceasedType);
        return FuneralMembershipCoverDto.builder()
                .membershipId(selectionId)
                .membershipNumber(membershipNumber)
                .burialSocietyName(burialSocietyName)
                .burialSocietyPartnerId(localPartnerId)
                .coverAmountCents(coverAmount)
                .funeralAmountCents(funeralAmount)
                .combinationAmountCents(combinationAmount)
                .coverSource(COVER_SOURCE_EXTERNAL)
                .sourceTenantId(tenantId)
                .sourceTenantName(tenantName)
                .sourceMembershipId(membershipId)
                .sourceReference(selectionId)
                .deceasedPartnerId(deceasedPartnerId)
                .deceasedType(deceasedType)
                .build();
    }

    private Map<String, FuneralMembershipCoverDto> resolveSelectedCovers(FuneralServiceEntity service, List<String> selectionIds) {
        Map<String, FuneralMembershipCoverDto> map = new HashMap<>();
        if (selectionIds == null) return map;

        List<String> externalIds = new ArrayList<>();
        for (String selectionId : selectionIds) {
            if (selectionId == null) continue;
            if (selectionId.startsWith("EXTERNAL:")) {
                String[] parts = selectionId.split(":", 6);
                if (parts.length >= 5) {
                    map.put(selectionId, resolveLiveExternalCoverSelection(selectionId));
                } else {
                    // Backwards compatibility for legacy external-cover snapshots.
                    externalIds.add(selectionId.substring("EXTERNAL:".length()));
                }
            } else if (selectionId.startsWith("LOCAL:")) {
                FuneralMembershipCoverDto dto = resolveLocalCoverSelection(selectionId, service);
                map.put(selectionId, dto);
            }
        }
        if (!externalIds.isEmpty()) {
            externalMembershipCoverRepository.findByIdInAndStatus(externalIds, "ACTIVE")
                    .forEach(cover -> map.put(
                            "EXTERNAL:" + cover.getId(),
                            resolveLegacyExternalCoverSelection(cover)));
        }
        return map;
    }

    private FuneralMembershipCoverDto resolveLegacyExternalCoverSelection(
            FuneralExternalMembershipCoverEntity snapshot
    ) {
        FuneralTenantIntegrationConfigDto config = getTenantIntegrationConfiguration();
        String configuredTenant = requireConfiguredExternalTenant(config);
        if (!configuredTenant.equals(snapshot.getSourceTenantId())) {
            throw new IllegalArgumentException(
                    "Legacy external cover belongs to a tenant that is no longer configured");
        }
        FuneralMembershipCoverDto liveCover = findExternalMembershipCover(
                snapshot.getIdentityNumber(), config).stream()
                .filter(cover -> Objects.equals(
                        snapshot.getSourceMembershipId(), cover.getSourceMembershipId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Legacy external cover is no longer active. Re-run membership lookup."));
        return FuneralMembershipCoverDto.builder()
                .membershipId("EXTERNAL:" + snapshot.getId())
                .membershipNumber(liveCover.getMembershipNumber())
                .burialSocietyName(liveCover.getBurialSocietyName())
                .burialSocietyPartnerId(liveCover.getBurialSocietyPartnerId())
                .coverAmountCents(liveCover.getCoverAmountCents())
                .funeralAmountCents(liveCover.getFuneralAmountCents())
                .combinationAmountCents(liveCover.getCombinationAmountCents())
                .coverSource(COVER_SOURCE_EXTERNAL)
                .sourceTenantId(liveCover.getSourceTenantId())
                .sourceTenantName(liveCover.getSourceTenantName())
                .sourceMembershipId(liveCover.getSourceMembershipId())
                .sourceReference(liveCover.getSourceReference())
                .deceasedPartnerId(liveCover.getDeceasedPartnerId())
                .deceasedType(liveCover.getDeceasedType())
                .build();
    }

    private FuneralMembershipCoverDto resolveLiveExternalCoverSelection(String selectionId) {
        String[] parts = selectionId.split(":", 6);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid external membership selection id: " + selectionId);
        }
        String tenantId = parts[1];
        String membershipId = parts[2];
        String deceasedPartnerId = parts[3];
        String deceasedType = parts[4];

        FuneralTenantIntegrationConfigDto config = getTenantIntegrationConfiguration();
        String configuredTenant = requireConfiguredExternalTenant(config);
        if (!configuredTenant.equals(tenantId)) {
            throw new IllegalArgumentException("The selected membership belongs to an unconfigured external tenant");
        }

        String membershipTable = qualifiedTable(tenantId, "membership");
        String planTable = qualifiedTable(tenantId, "membership_plan");
        Map<String, Object> membership = jdbcTemplate.queryForMap("""
                SELECT m.id, m.membership_no, m.plan_id, m.member_id, mp.name AS plan_name
                  FROM %s m
                  JOIN %s mp ON mp.id = m.plan_id
                 WHERE m.id = ? AND m.status = 'ACTIVE'
                """.formatted(membershipTable, planTable), membershipId);
        String dependentType = validateSelectedDeceasedAgainstMembership(
                tenantId,
                membershipId,
                String.valueOf(membership.get("member_id")),
                deceasedPartnerId,
                deceasedType);
        Long funeralPayout = findExternalMembershipPlanPayout(tenantId, membership.get("plan_id"), "FUNERAL", dependentType);
        Long combinationPayout = findExternalMembershipPlanPayout(tenantId, membership.get("plan_id"), "COMBINATION", dependentType);

        return FuneralMembershipCoverDto.builder()
                .membershipId(selectionId)
                .sourceMembershipId(membershipId)
                .membershipNumber(String.valueOf(membership.get("membership_no")))
                .deceasedPartnerId(deceasedPartnerId)
                .deceasedType(deceasedType)
                .coverAmountCents(funeralPayout)
                .funeralAmountCents(funeralPayout)
                .combinationAmountCents(combinationPayout)
                .burialSocietyName(String.valueOf(membership.get("plan_name")))
                .burialSocietyPartnerId(config.getExternalTenantPartnerId())
                .coverSource(COVER_SOURCE_EXTERNAL)
                .sourceTenantId(tenantId)
                .sourceTenantName(defaultString(config.getExternalTenantName(), tenantId))
                .sourceReference(selectionId)
                .build();
    }

    private Long findExternalMembershipPlanPayout(
            String tenantId,
            Object planId,
            String claimType,
            String dependentType
    ) {
        if (planId == null) return 0L;
        String payoutTable = qualifiedTable(tenantId, "membership_plan_claim_payout");
        return jdbcTemplate.query("""
                SELECT COALESCE(MAX(payout_amount_cents), 0)
                  FROM %s
                 WHERE plan_id = ? AND claim_type = ? AND active = 1
                   AND dependent_type IN (?, ?, 'ANY')
                """.formatted(payoutTable), rs -> rs.next() ? rs.getLong(1) : 0L,
                planId, claimType, dependentType, genericDependentType(dependentType));
    }

    private String genericDependentType(String dependentType) {
        return "MAIN_MEMBER".equalsIgnoreCase(dependentType) ? "MAIN_MEMBER" : "DEPENDENT";
    }

    private String validateSelectedDeceasedAgainstMembership(
            String tenantId,
            String membershipId,
            String memberPartnerId,
            String deceasedPartnerId,
            String deceasedType
    ) {
        if ("MAIN_MEMBER".equalsIgnoreCase(deceasedType)) {
            if (!Objects.equals(memberPartnerId, deceasedPartnerId)) {
                throw new IllegalArgumentException("Selected deceased is not the main member of the membership");
            }
            return "MAIN_MEMBER";
        }
        String dependentTable = tenantId == null
                ? "membership_dependent"
                : qualifiedTable(tenantId, "membership_dependent");
        try {
            String relationship = jdbcTemplate.queryForObject(
                    "SELECT relationship FROM " + dependentTable
                            + " WHERE membership_id = ? AND dependent_partner_id = ? AND active = 1",
                    String.class,
                    membershipId,
                    deceasedPartnerId);
            return StringUtils.hasText(relationship)
                    ? relationship.trim().toUpperCase(Locale.ROOT)
                    : "DEPENDENT";
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("Selected deceased is not an active dependent on the membership");
        }
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
                SELECT m.id, m.membership_no, m.plan_id, m.member_id, mp.name AS plan_name
                  FROM membership m
                  JOIN membership_plan mp ON mp.id = m.plan_id
                 WHERE m.id = ? AND m.status = 'ACTIVE'
                """, membershipId);
        String dependentType = validateSelectedDeceasedAgainstMembership(
                null,
                membershipId,
                String.valueOf(membership.get("member_id")),
                deceasedPartnerId,
                deceasedType);
        Long funeralPayout = findMembershipPlanPayout(membership.get("plan_id"), "FUNERAL", dependentType);
        Long combinationPayout = findMembershipPlanPayout(membership.get("plan_id"), "COMBINATION", dependentType);
        return FuneralMembershipCoverDto.builder()
                .membershipId(selectionId)
                .sourceMembershipId(membershipId)
                .membershipNumber(String.valueOf(membership.get("membership_no")))
                .deceasedPartnerId(deceasedPartnerId)
                .deceasedType(deceasedType)
                .coverAmountCents(funeralPayout)
                .funeralAmountCents(funeralPayout)
                .combinationAmountCents(combinationPayout)
                .burialSocietyName(String.valueOf(membership.get("plan_name")))
                .coverSource(COVER_SOURCE_LOCAL)
                .build();
    }

    private Long findMembershipPlanPayout(Object planId, String claimType, String dependentType) {
        if (planId == null) return 0L;
        return jdbcTemplate.query("""
                SELECT COALESCE(MAX(payout_amount_cents), 0)
                  FROM membership_plan_claim_payout
                 WHERE plan_id = ? AND claim_type = ? AND active = 1
                   AND dependent_type IN (?, ?, 'ANY')
                """, rs -> rs.next() ? rs.getLong(1) : 0L,
                planId, claimType, dependentType, genericDependentType(dependentType));
    }

    private List<FuneralInvoiceSplitDto> buildSplitsFromApprovedClaims(FuneralServiceEntity service) {
        List<FuneralClaimDto> claims = getClaims(service.getId());
        // Only approved cover may be invoiced to a burial society. Unresolved claims
        // remain excluded from cover splits and the outstanding funeral balance is
        // invoiced to the family representative. This matches the wizard's explicit
        // "Proceed to Generate" option for pending claims.
        long remaining = defaultLong(service.getTotalAmountCents());
        List<FuneralInvoiceSplitDto> splits = new ArrayList<>();
        for (FuneralClaimDto claim : claims) {
            if (remaining <= 0) break;
            if ("GROCERY".equalsIgnoreCase(defaultString(claim.getClaimType(), ""))) continue;
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
        long invoiceAmount = defaultLong(split.getAmountCents());
        boolean coveredByApprovedClaim = "BURIAL_SOCIETY".equalsIgnoreCase(split.getEntityType());
        long paidAmount = 0L;
        long balanceAmount = Math.max(0L, invoiceAmount - paidAmount);
        String status = "ISSUED";
        LocalDate invoiceDate = LocalDate.now();
        LocalDate dueDate = service.getFuneralDate() == null ? invoiceDate : service.getFuneralDate();
        String externalReference = truncate(defaultString(service.getDeceasedName(), service.getServiceRequestNo()), 100);
        String notes = "Funeral service " + defaultString(service.getServiceRequestNo(), service.getId())
                + " for " + defaultString(service.getDeceasedName(), "deceased")
                + ". " + defaultString(split.getDescription(), "");

        jdbcTemplate.update("""
                INSERT INTO invoice
                (id, invoice_no, external_ref, source_type, source_id, partner_id, invoice_date, due_date, status,
                 subtotal_cents, tax_cents, discount_cents, total_cents, paid_cents, balance_cents, currency, notes, created_at)
                VALUES (?, ?, ?, 'FUNERAL_SERVICE', ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, 'ZAR', ?, CURRENT_TIMESTAMP)
                """,
                invoiceId,
                invoiceNo,
                externalReference,
                service.getId(),
                split.getPartnerId(),
                invoiceDate,
                dueDate,
                status,
                invoiceAmount,
                invoiceAmount,
                paidAmount,
                balanceAmount,
                notes);

        FuneralPackageEntity funeralPackage = funeralPackageRepository.findById(service.getPackageId()).orElse(null);
        String packageName = funeralPackage == null ? "FUNERAL SERVICE" : defaultString(funeralPackage.getName(), "FUNERAL SERVICE");
        String primaryDescription = packageName.toUpperCase(Locale.ROOT).contains("FUNERAL SERVICE")
                ? packageName.toUpperCase(Locale.ROOT)
                : packageName.toUpperCase(Locale.ROOT) + " FUNERAL SERVICE";
        List<Map<String,Object>> packageItems = funeralPackage == null ? List.of() : jdbcTemplate.queryForList(
                "SELECT product_description,quantity,unit_price_cents,line_total_cents FROM funeral_package_item WHERE funeral_package_id=? ORDER BY product_description", funeralPackage.getId());
        if (packageItems.isEmpty()) {
            insertInvoiceLine(invoiceId, primaryDescription, 1.0, invoiceAmount);
        } else {
            long packageTotal = packageItems.stream().mapToLong(item -> ((Number)item.get("line_total_cents")).longValue()).sum();
            for (Map<String,Object> item : packageItems) {
                long lineTotal=((Number)item.get("line_total_cents")).longValue();
                long allocated=packageTotal<=0?0:Math.round((double)invoiceAmount*lineTotal/packageTotal);
                insertInvoiceLine(invoiceId, Objects.toString(item.get("product_description"),"Product"), ((Number)item.get("quantity")).doubleValue(), allocated);
            }
        }
        for (FuneralExtraDto extra : parseFuneralExtras(service.getExtrasJson())) {
            if (extra != null && StringUtils.hasText(extra.getDescription())) {
                insertInvoiceLine(invoiceId, extra.getDescription().trim().toUpperCase(Locale.ROOT), 1.0, 0L);
            }
        }

        return invoiceId;
    }

    private void insertInvoiceLine(String invoiceId, String description, double quantity, long amountCents) {
        jdbcTemplate.update("""
                INSERT INTO invoice_line
                (id, invoice_id, description, quantity, unit_price_cents, discount_cents, tax_cents, subtotal_cents, total_cents, created_at)
                VALUES (?, ?, ?, ?, ?, 0, 0, ?, ?, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID().toString(), invoiceId, truncate(defaultString(description, "Funeral service"), 255),
                quantity, amountCents, amountCents, amountCents);
    }

    private List<String> parsePackageInclusions(String inclusionsJson) {
        if (!StringUtils.hasText(inclusionsJson)) return List.of();
        try {
            List<String> inclusions = objectMapper.readValue(inclusionsJson, new TypeReference<List<String>>() {});
            return inclusions == null ? List.of() : inclusions.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private List<FuneralExtraDto> parseFuneralExtras(String extrasJson) {
        if (!StringUtils.hasText(extrasJson)) return List.of();
        try {
            List<FuneralExtraDto> extras = objectMapper.readValue(extrasJson, new TypeReference<List<FuneralExtraDto>>() {});
            return extras == null ? List.of() : extras;
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private FuneralClaimDto readClaimDto(String membershipClaimId) {
        Optional<FuneralServiceClaimEntity> linkOpt = funeralServiceClaimRepository.findByMembershipClaimId(membershipClaimId);
        FuneralServiceClaimEntity link = linkOpt.orElse(null);
        boolean externalClaim = link != null && isExternalClaimStorage(link);
        String claimTenantId = externalClaim ? requireExternalClaimTenant(link) : null;
        if (externalClaim) {
            ensureExternalStatusSyncAllowed(claimTenantId);
        }
        String claimTable = externalClaim
                ? qualifiedTable(claimTenantId, "membership_claim")
                : "membership_claim";
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM " + claimTable + " WHERE id = ?",
                membershipClaimId);
        String membershipId = String.valueOf(row.get("membership_id"));
        return FuneralClaimDto.builder()
                .funeralServiceClaimId(link == null ? null : link.getId())
                .membershipClaimId(String.valueOf(row.get("id")))
                .claimNo(String.valueOf(row.get("claim_no")))
                .membershipId(membershipId)
                .membershipNumber(externalClaim
                        ? resolveExternalMembershipNo(claimTenantId, membershipId)
                        : resolveMembershipNo(membershipId))
                .claimType(String.valueOf(row.get("claim_type")))
                .deceasedType(String.valueOf(row.get("deceased_type")))
                .deceasedPartnerId(String.valueOf(row.get("deceased_partner_id")))
                .claimantPartnerId(row.get("claimant_partner_id") == null ? null : String.valueOf(row.get("claimant_partner_id")))
                .dateOfDeath(asLocalDate(row.get("date_of_death")))
                .claimedAmountCents(asLong(row.get("claim_amount_cents")))
                .approvedAmountCents(resolveApprovedAmount(row))
                .status(String.valueOf(row.get("status")))
                .coverSource(link == null ? COVER_SOURCE_LOCAL : link.getCoverSource())
                .claimStorageScope(link == null
                        ? "LOCAL"
                        : defaultString(link.getClaimStorageScope(), "LOCAL"))
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
        Object approvedValue = row.get("approved_amount_cents");
        Long approvedAmount = approvedValue == null ? null : asLong(approvedValue);
        return approvedAmount == null || approvedAmount <= 0 ? asLong(row.get("claim_amount_cents")) : approvedAmount;
    }

    private String resolveInvoicePartnerForClaim(FuneralClaimDto claim) {
        Optional<FuneralServiceClaimEntity> link = funeralServiceClaimRepository.findByMembershipClaimId(claim.getMembershipClaimId());
        if (link.isPresent() && link.get().getBurialSocietyPartnerId() != null && !link.get().getBurialSocietyPartnerId().isBlank()) {
            return link.get().getBurialSocietyPartnerId();
        }
        // Fallback: invoice the membership owner for same-tenant records until group society partner mapping is added.
        return jdbcTemplate.queryForObject("SELECT member_id FROM membership WHERE id = ?", String.class, claim.getMembershipId());
    }


    private Map<String,String> resolveInvoiceIdentity(FuneralServiceEntity service, FuneralInvoiceSplitDto split) {
        Map<String,String> result=new HashMap<>();
        if (split.getMembershipClaimId()!=null) {
            FuneralServiceClaimEntity link=funeralServiceClaimRepository.findByMembershipClaimId(split.getMembershipClaimId()).orElse(null);
            String claimTable="membership_claim",membershipTable="membership",partnerTable="partner",identityTable="partner_identity";
            if(link!=null&&isExternalClaimStorage(link)){String t=requireExternalClaimTenant(link);claimTable=qualifiedTable(t,"membership_claim");membershipTable=qualifiedTable(t,"membership");partnerTable=qualifiedTable(t,"partner");identityTable=qualifiedTable(t,"partner_identity");}
            List<Map<String,Object>> rows=jdbcTemplate.queryForList("SELECT TRIM(CONCAT_WS(' ', NULLIF(p.name2,''), NULLIF(p.name3,''), NULLIF(p.name1,''))) holder_name,(SELECT pi.value FROM "+identityTable+" pi WHERE pi.partner=p.id ORDER BY CASE WHEN pi.type='SA-ID' THEN 0 WHEN pi.type='PASSPORT' THEN 1 ELSE 2 END, pi.type, pi.value LIMIT 1) holder_identity FROM "+claimTable+" c JOIN "+membershipTable+" m ON m.id=c.membership_id JOIN "+partnerTable+" p ON p.id=m.member_id WHERE c.id=?",split.getMembershipClaimId());
            if(!rows.isEmpty()){result.put("holderName",Objects.toString(rows.get(0).get("holder_name"),null));result.put("holderIdentity",Objects.toString(rows.get(0).get("holder_identity"),null));}
        }
        result.put("deceasedName",service.getDeceasedName());result.put("deceasedIdentity",service.getDeceasedIdentityNumber());return result;
    }

    private void createGroceryClaimForFuneral(FuneralServiceEntity service, FuneralMembershipCoverDto cover, InitiateFuneralClaimsDto request) {
        String tenantId = COVER_SOURCE_EXTERNAL.equals(cover.getCoverSource()) ? cover.getSourceTenantId() : null;
        if (tenantId != null) ensureExternalClaimCreationAllowed(tenantId);
        String table = tenantId == null ? "membership_claim" : qualifiedTable(tenantId, "membership_claim");
        String groceryId=UUID.randomUUID().toString(); String groceryNo=tenantId==null?generateMembershipClaimNo():generateExternalMembershipClaimNo(tenantId);
        long groceryAmount=findGroceryBenefitAmount(tenantId, cover.getSourceMembershipId(), cover.getDeceasedType());
        jdbcTemplate.update("""
            INSERT INTO %s(id,claim_no,membership_id,claim_type,deceased_type,deceased_partner_id,date_of_death,claim_date,cause_of_death,death_certificate_no,claimant_partner_id,claim_amount_cents,funeral_service_id,funeral_provider_tenant_id,status,notes,created_at)
            VALUES(?,?,?,'GROCERY',?,?,?,?,?,?,?,?,?,?,'DRAFT',?,CURRENT_TIMESTAMP)
            """.formatted(table),groceryId,groceryNo,cover.getSourceMembershipId(),defaultString(cover.getDeceasedType(),"MAIN_MEMBER"),
            defaultString(cover.getDeceasedPartnerId(),service.getDeceasedPartnerId()),service.getFuneralDate()==null?LocalDate.now():service.getFuneralDate(),LocalDate.now(),
            defaultString(request.getCauseOfDeath(),service.getCauseOfDeath()),defaultString(request.getDeathCertificateNo(),service.getDeathCertificateNo()),service.getFamilyRepId(),groceryAmount,service.getId(),TenantContext.getCurrentTenant(),
            "Automatically created from funeral service "+service.getServiceRequestNo());
        FuneralServiceClaimEntity groceryLink=new FuneralServiceClaimEntity(); groceryLink.setFuneralServiceId(service.getId()); groceryLink.setMembershipClaimId(groceryId);
        groceryLink.setClaimStorageScope(tenantId==null?"LOCAL":"EXTERNAL"); groceryLink.setClaimOwnerTenantId(tenantId==null?TenantContext.getCurrentTenant():tenantId);
        groceryLink.setCoverSource(cover.getCoverSource()); groceryLink.setSourceTenantId(cover.getSourceTenantId()); groceryLink.setSourceTenantName(cover.getSourceTenantName());
        groceryLink.setSourceMembershipId(cover.getSourceMembershipId()); groceryLink.setSourceReference(cover.getSourceReference()); groceryLink.setBurialSocietyPartnerId(cover.getBurialSocietyPartnerId());
        funeralServiceClaimRepository.save(groceryLink);
        prepareFuneralClaimForm(service, groceryId, groceryNo, "GROCERY", groceryAmount);
    }

    private void prepareFuneralClaimForm(FuneralServiceEntity service, String claimId, String claimNo,
                                         String claimType, long amountCents) {
        String claimantName = resolvePartnerName(service.getFamilyRepId());
        claimFormGenerationService.generateForFuneralClaim(
                claimId, claimNo, claimType, service.getDeceasedName(), claimantName, amountCents);
    }

    private String resolvePartnerName(String partnerId) {
        if (!StringUtils.hasText(partnerId)) return "";
        List<String> names = jdbcTemplate.query("SELECT TRIM(CONCAT_WS(' ', NULLIF(name2,''), NULLIF(name3,''), NULLIF(name1,''))) FROM partner WHERE id=?",
                (rs, rowNum) -> rs.getString(1), partnerId);
        return names.isEmpty() ? partnerId : names.get(0);
    }

    private long findGroceryBenefitAmount(String tenantId, String membershipId, String deceasedType) {
        String membershipTable = tenantId == null
                ? "membership"
                : qualifiedTable(tenantId, "membership");
        String payoutTable = tenantId == null
                ? "membership_plan_claim_payout"
                : qualifiedTable(tenantId, "membership_plan_claim_payout");
        String normalizedDependentType = defaultString(deceasedType, "MAIN_MEMBER").toUpperCase(Locale.ROOT);
        String genericType = genericDependentType(normalizedDependentType);

        List<Long> values = jdbcTemplate.query("""
                SELECT COALESCE(MAX(p.payout_amount_cents), 0)
                  FROM %s m
                  JOIN %s p ON p.plan_id = m.plan_id
                 WHERE m.id = ?
                   AND p.claim_type = 'GROCERY'
                   AND p.active = 1
                   AND p.dependent_type IN (?, ?, 'ANY')
                """.formatted(membershipTable, payoutTable),
                (rs, rowNum) -> rs.getLong(1),
                membershipId,
                normalizedDependentType,
                genericType);
        return values.isEmpty() ? 0L : values.get(0);
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
            List<FuneralClaimDto> claims = funeralServiceClaimRepository.findByFuneralServiceId(link.getFuneralServiceId())
                    .stream()
                    .map(claimLink -> readClaimDto(claimLink.getMembershipClaimId()))
                    .collect(Collectors.toList());
            refreshFuneralServiceStatus(link.getFuneralServiceId(), claims);
        });
    }

    private void refreshFuneralServiceStatus(String funeralServiceId, List<FuneralClaimDto> claims) {
        if (claims == null || claims.isEmpty()) {
            return;
        }
        FuneralServiceEntity service = getFuneralServiceOrThrow(funeralServiceId);
        if ("INVOICED".equalsIgnoreCase(defaultString(service.getStatus(), ""))) {
            return;
        }
        boolean anyPending = claims.stream()
                .anyMatch(claim -> List.of("DRAFT", "SUBMITTED")
                        .contains(defaultString(claim.getStatus(), "").toUpperCase(Locale.ROOT)));
        String targetStatus = anyPending ? "CLAIMS_INITIATED" : "CLAIMS_RESOLVED";
        if (!targetStatus.equalsIgnoreCase(defaultString(service.getStatus(), ""))) {
            service.setStatus(targetStatus);
            funeralServiceRepository.save(service);
        }
    }

    private FuneralServiceRequestResponseDto toServiceResponse(FuneralServiceEntity entity) {
        return FuneralServiceRequestResponseDto.builder()
                .id(entity.getId())
                .serviceRequestNo(entity.getServiceRequestNo())
                .mortuaryInventoryId(entity.getMortuaryInventoryId())
                .deceasedName(entity.getDeceasedName())
                .deceasedIdentityNumber(entity.getDeceasedIdentityNumber())
                .deceasedPartnerId(entity.getDeceasedPartnerId())
                .packageId(entity.getPackageId())
                .familyRepId(entity.getFamilyRepId())
                .funeralDate(entity.getFuneralDate())
                .funeralArea(entity.getFuneralArea())
                .deathCertificateNo(entity.getDeathCertificateNo())
                .causeOfDeath(entity.getCauseOfDeath())
                .totalAmountCents(entity.getTotalAmountCents())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
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

    private String generateMembershipClaimNo() {
        try {
            return numberAllocationService.allocateNumber("MEMBERSHIP_CLAIM");
        } catch (Exception ignored) {
            try {
                return numberAllocationService.allocateNumber("CLAIM");
            } catch (Exception ignoredAgain) {
                return "CLM-FUN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            }
        }
    }

    private String generateExternalMembershipClaimNo(String tenantId) {
        String sequenceTable = qualifiedTable(tenantId, "number_sequence");
        try {
            Long nextNo = jdbcTemplate.queryForObject(
                    "SELECT next_no FROM " + sequenceTable + " WHERE seq_type = 'MEMBERSHIP_CLAIM' FOR UPDATE",
                    Long.class);
            if (nextNo == null) {
                throw new IllegalStateException("MEMBERSHIP_CLAIM sequence is not configured");
            }
            jdbcTemplate.update(
                    "UPDATE " + sequenceTable + " SET next_no = ? WHERE seq_type = 'MEMBERSHIP_CLAIM'",
                    nextNo + 1);
            return nextNo.toString();
        } catch (Exception primaryFailure) {
            try {
                Long nextNo = jdbcTemplate.queryForObject(
                        "SELECT next_no FROM " + sequenceTable + " WHERE seq_type = 'CLAIM' FOR UPDATE",
                        Long.class);
                if (nextNo == null) throw new IllegalStateException("CLAIM sequence is not configured");
                jdbcTemplate.update(
                        "UPDATE " + sequenceTable + " SET next_no = ? WHERE seq_type = 'CLAIM'",
                        nextNo + 1);
                return nextNo.toString();
            } catch (Exception ignored) {
                return "CLM-EXT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            }
        }
    }

    private String generateFuneralServiceRequestNo() {
        try {
            return numberAllocationService.allocateNumber("FUNERAL_SERVICE_REQUEST");
        } catch (Exception ignored) {
            try {
                return numberAllocationService.allocateNumber("SERVICE-REQUEST");
            } catch (Exception ignoredAgain) {
                return "FSR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            }
        }
    }

    private String generateInvoiceNo() {
        try {
            return numberRangeService.generateNumber("INVOICE");
        } catch (Exception ignored) {
            try {
                return numberAllocationService.allocateNumber("INVOICE");
            } catch (Exception ignoredAgain) {
                return "INV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            }
        }
    }

    private String buildClaimNotes(FuneralServiceEntity service, String notes, boolean externalClaim) {
        if (!externalClaim) {
            return notes;
        }
        String integrationReference = "Funeral arrangement "
                + defaultString(service.getServiceRequestNo(), service.getId())
                + " initiated by tenant "
                + defaultString(TenantContext.getCurrentTenant(), "UNKNOWN");
        return StringUtils.hasText(notes)
                ? integrationReference + ". " + notes.trim()
                : integrationReference;
    }

    private String resolveMembershipNo(String membershipId) {
        try {
            return jdbcTemplate.queryForObject("SELECT membership_no FROM membership WHERE id = ?", String.class, membershipId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveExternalMembershipNo(String tenantId, String membershipId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT membership_no FROM " + qualifiedTable(tenantId, "membership") + " WHERE id = ?",
                    String.class,
                    membershipId);
        } catch (Exception ignored) {
            return null;
        }
    }

    public List<TenantTrustRelationshipDto> getTrustedTenantRelationships() {
        String currentTenant = TenantContext.getCurrentTenant();
        String table = qualifiedTable(currentTenant, "tenant_trust_relationship");
        return jdbcTemplate.query("SELECT * FROM " + table + " WHERE requester_tenant_id = ? OR provider_tenant_id = ? ORDER BY requested_at DESC",
                (rs, rowNum) -> TenantTrustRelationshipDto.builder()
                        .id(rs.getString("id"))
                        .requesterTenantId(rs.getString("requester_tenant_id"))
                        .requesterTenantName(rs.getString("requester_tenant_name"))
                        .providerTenantId(rs.getString("provider_tenant_id"))
                        .providerTenantName(rs.getString("provider_tenant_name"))
                        .integrationType(rs.getString("integration_type"))
                        .status(rs.getString("status"))
                        .membershipLookupAllowed(rs.getBoolean("allow_membership_lookup"))
                        .claimCreationAllowed(rs.getBoolean("allow_claim_creation"))
                        .claimStatusReadAllowed(rs.getBoolean("allow_claim_status_read"))
                        .settlementAllowed(rs.getBoolean("allow_settlement"))
                        .requestedAt(rs.getTimestamp("requested_at") == null ? null : rs.getTimestamp("requested_at").toLocalDateTime())
                        .approvedAt(rs.getTimestamp("approved_at") == null ? null : rs.getTimestamp("approved_at").toLocalDateTime())
                        .revokedAt(rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime())
                        .build(), currentTenant, currentTenant);
    }

    public TenantTrustRelationshipDto requestTrustedTenantRelationship(TenantTrustRelationshipDto request) {
        String requester = TenantContext.getCurrentTenant();
        String provider = trimToNull(request.getProviderTenantId());
        validateRequired(provider, "providerTenantId");
        if (requester.equals(provider)) throw new IllegalArgumentException("Provider tenant must differ from requester tenant");
        if (!schemaExists(provider)) throw new IllegalArgumentException("Provider tenant schema is unavailable: " + provider);
        String requesterName = tenantName(requester);
        String providerName = tenantName(provider);
        LocalDateTime now = LocalDateTime.now();
        List<String> existing = jdbcTemplate.query("SELECT id FROM " + qualifiedTable(requester, "tenant_trust_relationship") +
                        " WHERE requester_tenant_id = ? AND provider_tenant_id = ? AND integration_type = 'FUNERAL_MEMBERSHIP_CLAIMS'",
                (rs, n) -> rs.getString(1), requester, provider);
        String id = existing.isEmpty() ? UUID.randomUUID().toString().replace("-", "") : existing.get(0);
        if (existing.isEmpty()) {
            insertTrustRow(requester, id, requester, requesterName, provider, providerName, "PENDING", request, now);
            insertTrustRow(provider, id, requester, requesterName, provider, providerName, "PENDING", request, now);
        } else {
            resetTrustRequest(requester, id, request, now);
            resetTrustRequest(provider, id, request, now);
        }
        return findTrustById(requester, id);
    }

    public TenantTrustRelationshipDto updateTrustedTenantRelationshipStatus(String id, String status) {
        String current = TenantContext.getCurrentTenant();
        TenantTrustRelationshipDto trust = findTrustById(current, id);
        String normalized = defaultString(status, "").trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED", "SUSPENDED", "REVOKED").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported trust status: " + status);
        }
        if (("APPROVED".equals(normalized) || "REJECTED".equals(normalized) || "SUSPENDED".equals(normalized))
                && !current.equals(trust.getProviderTenantId())) {
            throw new IllegalArgumentException("Only the provider tenant may " + normalized.toLowerCase(Locale.ROOT) + " this relationship");
        }
        if ("REVOKED".equals(normalized) && !current.equals(trust.getRequesterTenantId()) && !current.equals(trust.getProviderTenantId())) {
            throw new IllegalArgumentException("Only a party to the relationship may revoke it");
        }
        LocalDateTime now = LocalDateTime.now();
        updateTrustStatus(trust.getRequesterTenantId(), id, normalized, now);
        updateTrustStatus(trust.getProviderTenantId(), id, normalized, now);
        return findTrustById(current, id);
    }

    private void insertTrustRow(String schema, String id, String requester, String requesterName, String provider,
                                String providerName, String status, TenantTrustRelationshipDto request, LocalDateTime now) {
        jdbcTemplate.update("INSERT INTO " + qualifiedTable(schema, "tenant_trust_relationship") +
                        " (id, requester_tenant_id, requester_tenant_name, provider_tenant_id, provider_tenant_name, integration_type, status, allow_membership_lookup, allow_claim_creation, allow_claim_status_read, allow_settlement, requested_at, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, requester, requesterName, provider, providerName, "FUNERAL_MEMBERSHIP_CLAIMS", status,
                defaultBoolean(request.getMembershipLookupAllowed(), true), defaultBoolean(request.getClaimCreationAllowed(), true),
                defaultBoolean(request.getClaimStatusReadAllowed(), true), defaultBoolean(request.getSettlementAllowed(), false),
                now, now, now);
    }

    private void resetTrustRequest(String schema, String id, TenantTrustRelationshipDto request, LocalDateTime now) {
        jdbcTemplate.update("UPDATE " + qualifiedTable(schema, "tenant_trust_relationship") +
                        " SET status = 'PENDING', allow_membership_lookup = ?, allow_claim_creation = ?, allow_claim_status_read = ?, allow_settlement = ?, requested_at = ?, approved_at = NULL, revoked_at = NULL, updated_at = ? WHERE id = ?",
                defaultBoolean(request.getMembershipLookupAllowed(), true), defaultBoolean(request.getClaimCreationAllowed(), true),
                defaultBoolean(request.getClaimStatusReadAllowed(), true), defaultBoolean(request.getSettlementAllowed(), false),
                now, now, id);
    }

    private void updateTrustStatus(String schema, String id, String status, LocalDateTime now) {
        String approved = "APPROVED".equals(status) ? ", approved_at = ?" : "";
        String revoked = "REVOKED".equals(status) ? ", revoked_at = ?" : "";
        List<Object> args = new ArrayList<>(); args.add(status); args.add(now);
        if ("APPROVED".equals(status)) args.add(now);
        if ("REVOKED".equals(status)) args.add(now);
        args.add(id);
        jdbcTemplate.update("UPDATE " + qualifiedTable(schema, "tenant_trust_relationship") + " SET status = ?, updated_at = ?" + approved + revoked + " WHERE id = ?", args.toArray());
    }

    private TenantTrustRelationshipDto findTrustById(String schema, String id) {
        return jdbcTemplate.queryForObject("SELECT * FROM " + qualifiedTable(schema, "tenant_trust_relationship") + " WHERE id = ?",
                (rs, n) -> TenantTrustRelationshipDto.builder().id(rs.getString("id"))
                        .requesterTenantId(rs.getString("requester_tenant_id")).requesterTenantName(rs.getString("requester_tenant_name"))
                        .providerTenantId(rs.getString("provider_tenant_id")).providerTenantName(rs.getString("provider_tenant_name"))
                        .integrationType(rs.getString("integration_type")).status(rs.getString("status"))
                        .membershipLookupAllowed(rs.getBoolean("allow_membership_lookup")).claimCreationAllowed(rs.getBoolean("allow_claim_creation"))
                        .claimStatusReadAllowed(rs.getBoolean("allow_claim_status_read")).settlementAllowed(rs.getBoolean("allow_settlement"))
                        .requestedAt(rs.getTimestamp("requested_at") == null ? null : rs.getTimestamp("requested_at").toLocalDateTime())
                        .approvedAt(rs.getTimestamp("approved_at") == null ? null : rs.getTimestamp("approved_at").toLocalDateTime())
                        .revokedAt(rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime()).build(), id);
    }

    private String tenantName(String tenantId) {
        return tenantAdminService.getAll().stream().filter(t -> tenantId.equals(t.getId())).map(t -> defaultString(t.getName(), tenantId)).findFirst().orElse(tenantId);
    }

    private void requireApprovedTrust(String providerTenantId, String permissionColumn) {
        String requester = TenantContext.getCurrentTenant();
        if (!List.of("allow_membership_lookup", "allow_claim_creation", "allow_claim_status_read", "allow_settlement").contains(permissionColumn)) {
            throw new IllegalArgumentException("Invalid trust permission");
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + qualifiedTable(requester, "tenant_trust_relationship") +
                        " WHERE requester_tenant_id = ? AND provider_tenant_id = ? AND integration_type = 'FUNERAL_MEMBERSHIP_CLAIMS' AND status = 'APPROVED' AND " + permissionColumn + " = 1",
                Integer.class, requester, providerTenantId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("No approved trusted-tenant relationship permits this operation");
        }
    }

    private FuneralTenantIntegrationConfigDto toTenantIntegrationConfigDto(FuneralTenantIntegrationConfigEntity entity) {
        return FuneralTenantIntegrationConfigDto.builder()
                .membershipSourceMode(normalizeSourceMode(entity.getMembershipSourceMode()))
                .externalTenantId(entity.getExternalTenantId())
                .externalTenantName(entity.getExternalTenantName())
                .externalTenantPartnerId(entity.getExternalTenantPartnerId())
                .membershipLookupEnabled(defaultBoolean(entity.getMembershipLookupEnabled(), true))
                .claimCreationEnabled(defaultBoolean(entity.getClaimCreationEnabled(), true))
                .claimStatusSyncEnabled(defaultBoolean(entity.getClaimStatusSyncEnabled(), true))
                .active(defaultBoolean(entity.getActive(), true))
                .build();
    }

    private String normalizeSourceMode(String mode) {
        String normalized = defaultString(mode, SOURCE_MODE_LOCAL_ONLY).trim().toUpperCase(Locale.ROOT);
        if (!List.of(SOURCE_MODE_LOCAL_ONLY, SOURCE_MODE_EXTERNAL_ONLY, SOURCE_MODE_LOCAL_AND_EXTERNAL).contains(normalized)) {
            throw new IllegalArgumentException("Unsupported membershipSourceMode: " + mode);
        }
        return normalized;
    }

    private boolean includesLocalSource(FuneralTenantIntegrationConfigDto config) {
        String mode = normalizeSourceMode(config == null ? null : config.getMembershipSourceMode());
        return SOURCE_MODE_LOCAL_ONLY.equals(mode) || SOURCE_MODE_LOCAL_AND_EXTERNAL.equals(mode);
    }

    private boolean includesExternalSource(FuneralTenantIntegrationConfigDto config) {
        String mode = normalizeSourceMode(config == null ? null : config.getMembershipSourceMode());
        return SOURCE_MODE_EXTERNAL_ONLY.equals(mode) || SOURCE_MODE_LOCAL_AND_EXTERNAL.equals(mode);
    }

    private String requireConfiguredExternalTenant(FuneralTenantIntegrationConfigDto config) {
        if (config == null || !includesExternalSource(config) || !Boolean.TRUE.equals(config.getActive())) {
            throw new IllegalArgumentException("External membership integration is not active");
        }
        String tenantId = trimToNull(config.getExternalTenantId());
        validateRequired(tenantId, "externalTenantId");
        if (!schemaExists(tenantId)) {
            throw new IllegalArgumentException("External tenant schema is not available: " + tenantId);
        }
        return tenantId;
    }

    private void ensureExternalClaimCreationAllowed(String tenantId) {
        FuneralTenantIntegrationConfigDto config = getTenantIntegrationConfiguration();
        String configuredTenant = requireConfiguredExternalTenant(config);
        if (!configuredTenant.equals(tenantId)) {
            throw new IllegalArgumentException("Claim tenant is not configured for this funeral tenant");
        }
        if (!Boolean.TRUE.equals(config.getClaimCreationEnabled())) {
            throw new IllegalArgumentException("External claim creation is disabled");
        }
        requireApprovedTrust(tenantId, "allow_claim_creation");
    }

    private void ensureExternalStatusSyncAllowed(String tenantId) {
        FuneralTenantIntegrationConfigDto config = getTenantIntegrationConfiguration();
        String configuredTenant = requireConfiguredExternalTenant(config);
        if (!configuredTenant.equals(tenantId)) {
            throw new IllegalArgumentException("Claim tenant is not configured for this funeral tenant");
        }
        if (!Boolean.TRUE.equals(config.getClaimStatusSyncEnabled())) {
            throw new IllegalArgumentException("External claim status synchronisation is disabled");
        }
        requireApprovedTrust(tenantId, "allow_claim_status_read");
    }

    private boolean isExternalClaimStorage(FuneralServiceClaimEntity link) {
        return link != null && "EXTERNAL".equalsIgnoreCase(link.getClaimStorageScope());
    }

    private String requireExternalClaimTenant(FuneralServiceClaimEntity link) {
        if (link == null) {
            throw new IllegalArgumentException("External claim link is missing");
        }
        String tenantId = trimToNull(link.getClaimOwnerTenantId());
        if (tenantId == null) {
            tenantId = trimToNull(link.getSourceTenantId());
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("External claim tenant is missing from the funeral claim link");
        }
        return tenantId;
    }

    private boolean schemaExists(String tenantId) {
        if (!StringUtils.hasText(tenantId) || !tenantId.matches("[A-Za-z0-9_-]{1,128}")) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?",
                Integer.class,
                tenantId);
        return count != null && count > 0;
    }

    private String qualifiedTable(String tenantId, String tableName) {
        if (!StringUtils.hasText(tenantId) || !tenantId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("Invalid tenant identifier");
        }
        if (!StringUtils.hasText(tableName) || !tableName.matches("[A-Za-z0-9_]{1,128}")) {
            throw new IllegalArgumentException("Invalid table name");
        }
        return "`" + tenantId + "`.`" + tableName + "`";
    }

    private Boolean defaultBoolean(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean isApprovedStatus(String status) {
        return List.of("APPROVED", "PARTIALLY_APPROVED", "PAID").contains(defaultString(status, ""));
    }

    private void validateSelectedCoverLimit(List<String> selectedMemberships) {
        int maxSelectableCovers = getMaxSelectableCovers();
        if (maxSelectableCovers <= 0 || selectedMemberships == null) {
            return;
        }
        int selectedCount = (int) selectedMemberships.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .distinct()
                .count();
        if (selectedCount > maxSelectableCovers) {
            throw new IllegalArgumentException("A maximum of " + maxSelectableCovers + " cover(s) can be selected for a funeral service. You selected " + selectedCount + ".");
        }
    }

    private int getMaxSelectableCovers() {
        String value = settingService.getSetting(MAX_SELECTED_COVERS_ATTRIBUTE, FUNERAL_SERVICE_SETTING);
        return normalizeMaxSelectableCovers(parseInteger(value, 0));
    }

    private int normalizeMaxSelectableCovers(Integer value) {
        if (value == null || value <= 0) {
            return 0;
        }
        return Math.min(value, 99);
    }

    private int parseInteger(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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
