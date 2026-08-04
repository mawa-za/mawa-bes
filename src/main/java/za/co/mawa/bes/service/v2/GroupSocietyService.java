package za.co.mawa.bes.service.v2;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.v2.group.GroupSocietyClaimDebitRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyContactRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyMemberRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyMasterDataDto;
import za.co.mawa.bes.dto.v2.group.GroupSocietyPaymentRequest;
import za.co.mawa.bes.dto.v2.group.GroupSocietyRequest;
import za.co.mawa.bes.entity.v2.GroupSocietyAccountTxnEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyContactEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyMemberEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerViewEntity;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.dto.partner.PartnerInboundDto;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.service.PartnerServiceV2;
import za.co.mawa.bes.repository.v2.GroupSocietyAccountTxnRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyContactRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyMemberRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service(value = "GroupSocietyServiceV2")
public class GroupSocietyService {

    private final GroupSocietyRepository groupSocietyRepository;
    private final GroupSocietyContactRepository contactRepository;
    private final GroupSocietyMemberRepository memberRepository;
    private final GroupSocietyAccountTxnRepository accountTxnRepository;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final PartnerServiceV2 partnerServiceV2;
    private final ReferenceDataValidationService referenceDataValidationService;

    /*
     * Inject your existing PartnerRepository here if available.
     *
     * Example:
     *
     * private final PartnerRepository partnerRepository;
     *
     * Then validate:
     * PartnerEntity partner = partnerRepository.findById(request.getPartnerId())
     *      .orElseThrow(() -> new RuntimeException("Partner not found"));
     *
     * if (!"GROUP".equalsIgnoreCase(partner.getType())) {
     *      throw new RuntimeException("Partner must be of type GROUP");
     * }
     */

    public GroupSocietyService(
            GroupSocietyRepository groupSocietyRepository,
            GroupSocietyContactRepository contactRepository,
            GroupSocietyMemberRepository memberRepository,
            GroupSocietyAccountTxnRepository accountTxnRepository,
            PartnerRepository partnerRepository,
            ProductRepository productRepository,
            PartnerServiceV2 partnerServiceV2,
            ReferenceDataValidationService referenceDataValidationService
    ) {
        this.groupSocietyRepository = groupSocietyRepository;
        this.contactRepository = contactRepository;
        this.memberRepository = memberRepository;
        this.accountTxnRepository = accountTxnRepository;
        this.partnerRepository = partnerRepository;
        this.productRepository = productRepository;
        this.partnerServiceV2 = partnerServiceV2;
        this.referenceDataValidationService = referenceDataValidationService;
    }

    public List<GroupSocietyMasterDataDto> getMasterData(String status) {
        List<GroupSocietyEntity> societies = status == null || status.isBlank()
                || "ALL".equalsIgnoreCase(status)
                ? groupSocietyRepository.findAll()
                : groupSocietyRepository.findByStatus(status);
        enrichPartnerDetails(societies);
        enrichProductDetails(societies);

        return societies.stream().map(society -> GroupSocietyMasterDataDto.builder()
                .id(society.getId())
                .partnerId(society.getPartnerId())
                .partnerNo(society.getPartnerNumber())
                .productId(society.getProductId())
                .productCode(society.getProductCode())
                .productDescription(society.getProductDescription())
                .groupNo(society.getGroupNo())
                .name(society.getDisplayName())
                .societyType(society.getSocietyType())
                .status(society.getStatus())
                .availableBalanceCents(society.getAvailableBalanceCents())
                .totalPaidCents(society.getTotalPaidCents())
                .lastPaymentDate(society.getLastPaymentDate())
                .build()).toList();
    }

    public List<GroupSocietyEntity> getAll(String status, String societyType) {
        List<GroupSocietyEntity> societies;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            societies = groupSocietyRepository.findByStatus(status);
        } else if (societyType != null && !societyType.isBlank()) {
            societies = groupSocietyRepository.findBySocietyType(societyType);
        } else {
            societies = groupSocietyRepository.findAll();
        }
        enrichPartnerDetails(societies);
        enrichProductDetails(societies);
        return societies;
    }

    public GroupSocietyEntity getById(String id) {
        return enrichDetails(groupSocietyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group society not found: " + id)));
    }

    public GroupSocietyEntity getByGroupNo(String groupNo) {
        return enrichDetails(groupSocietyRepository.findByGroupNo(groupNo)
                .orElseThrow(() -> new RuntimeException("Group society not found: " + groupNo)));
    }

    public GroupSocietyEntity getByPartnerId(String partnerId) {
        return enrichDetails(groupSocietyRepository.findByPartnerId(partnerId)
                .orElseThrow(() -> new RuntimeException("Group society not found for partner: " + partnerId)));
    }

    @Transactional
    public GroupSocietyEntity create(GroupSocietyRequest request) {
        validateGroupSocietyRequest(request);

        if (groupSocietyRepository.existsByGroupNo(request.getGroupNo().trim())) {
            throw new RuntimeException("Group society number already exists: " + request.getGroupNo());
        }

        ProductEntity product = validateGroupSocietyProduct(request.getProductId());
        PartnerEntity partner = resolveOrCreateGroupPartner(request);
        if (groupSocietyRepository.existsByPartnerId(partner.getId())) {
            throw new RuntimeException("This group partner is already linked to a group society");
        }

        Long requestedOpeningBalance = safeLong(request.getOpeningBalanceCents());
        if (requestedOpeningBalance != 0L) {
            throw new IllegalArgumentException(
                    "Group societies must start with a zero balance. Attach supporting documents and submit a balance adjustment for approval after creation.");
        }

        GroupSocietyEntity entity = new GroupSocietyEntity();
        entity.setPartnerId(partner.getId());
        entity.setProductId(product.getId());
        entity.setGroupNo(request.getGroupNo().trim().toUpperCase());
        entity.setSocietyType(defaultValue(request.getSocietyType(), "GROUP").trim().toUpperCase());
        entity.setStatus("ACTIVE");
        entity.setAvailableBalanceCents(0L);
        entity.setTotalPaidCents(0L);
        entity.setTotalClaimedCents(0L);
        GroupSocietyEntity saved = groupSocietyRepository.save(entity);
        enrichPartnerDetails(saved, partner);
        return enrichProductDetails(saved, product);
    }

    @Transactional
    public GroupSocietyEntity update(String id, GroupSocietyRequest request) {
        GroupSocietyEntity entity = getById(id);

        if (request.getSocietyType() != null) {
            entity.setSocietyType(request.getSocietyType());
        }

        // Lifecycle status is intentionally not editable here. Activation,
        // suspension and closure must always pass through the approval workflow.
        return enrichDetails(groupSocietyRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        getById(id);
        throw new IllegalStateException(
                "Group societies cannot be deleted directly. Submit a closure request for approval instead.");
    }

    public List<GroupSocietyContactEntity> getContacts(String groupSocietyId) {
        getById(groupSocietyId);
        return contactRepository.findByGroupSocietyId(groupSocietyId);
    }

    @Transactional
    public GroupSocietyContactEntity addContact(String groupSocietyId, GroupSocietyContactRequest request) {
        getById(groupSocietyId);

        if (request.getContactName() == null || request.getContactName().isBlank()) {
            throw new RuntimeException("Contact name is required");
        }

        GroupSocietyContactEntity entity = new GroupSocietyContactEntity();
        entity.setGroupSocietyId(groupSocietyId);
        entity.setContactName(request.getContactName());
        entity.setRole(request.getRole());
        entity.setMobileNo(referenceDataValidationService.optionalContactNumber(request.getMobileNo()));
        entity.setEmail(request.getEmail());
        entity.setPrimaryContact(Boolean.TRUE.equals(request.getPrimaryContact()));

        return contactRepository.save(entity);
    }

    @Transactional
    public void deleteContact(String contactId) {
        contactRepository.deleteById(contactId);
    }

    public List<GroupSocietyMemberEntity> getMembers(String groupSocietyId, String status) {
        getById(groupSocietyId);

        if (status != null && !status.isBlank()) {
            return memberRepository.findByGroupSocietyIdAndStatus(groupSocietyId, status);
        }

        return memberRepository.findByGroupSocietyId(groupSocietyId);
    }

    @Transactional
    public GroupSocietyMemberEntity addMember(String groupSocietyId, GroupSocietyMemberRequest request) {
        getById(groupSocietyId);

        if (request.getMemberId() == null || request.getMemberId().isBlank()) {
            throw new RuntimeException("memberId is required");
        }

        if (memberRepository.existsByGroupSocietyIdAndMemberId(groupSocietyId, request.getMemberId())) {
            throw new RuntimeException("Member already exists in this group society");
        }

        GroupSocietyMemberEntity entity = new GroupSocietyMemberEntity();

        entity.setGroupSocietyId(groupSocietyId);
        entity.setMemberId(request.getMemberId());
        entity.setMembershipId(request.getMembershipId());
        entity.setEmployeeNo(request.getEmployeeNo());
        entity.setExternalRef(request.getExternalRef());
        entity.setJoinDate(request.getJoinDate() != null ? request.getJoinDate() : LocalDate.now());
        entity.setStatus(defaultValue(request.getStatus(), "ACTIVE"));

        return memberRepository.save(entity);
    }

    @Transactional
    public GroupSocietyMemberEntity removeMember(String groupSocietyId, String memberId) {
        GroupSocietyMemberEntity entity = memberRepository
                .findByGroupSocietyIdAndMemberId(groupSocietyId, memberId)
                .orElseThrow(() -> new RuntimeException("Member not found in group society"));

        entity.setStatus("EXITED");
        entity.setExitDate(LocalDate.now());

        return memberRepository.save(entity);
    }

    @Transactional
    public GroupSocietyAccountTxnEntity recordPayment(String groupSocietyId, GroupSocietyPaymentRequest request) {
        validateAmount(request.getAmountCents());

        GroupSocietyEntity society = getByIdForUpdate(groupSocietyId);
        validateGroupIsOpenForPosting(society);

        if (request.getReferenceId() != null &&
                accountTxnRepository.existsByReferenceTypeAndReferenceIdAndTxnType(
                        "RECEIPT",
                        request.getReferenceId(),
                        "PAYMENT"
                )) {
            throw new RuntimeException("Payment already posted for this receipt/reference");
        }

        Long balanceBefore = safeLong(society.getAvailableBalanceCents());
        Long amount = request.getAmountCents();
        Long balanceAfter = balanceBefore + amount;

        LocalDate paymentDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();

        society.setAvailableBalanceCents(balanceAfter);
        society.setTotalPaidCents(safeLong(society.getTotalPaidCents()) + amount);
        society.setLastPaymentDate(paymentDate);

        groupSocietyRepository.save(society);

        GroupSocietyAccountTxnEntity txn = new GroupSocietyAccountTxnEntity();
        txn.setGroupSocietyId(groupSocietyId);
        txn.setTxnType("PAYMENT");
        txn.setDirection("CREDIT");
        txn.setAmountCents(amount);
        txn.setBalanceBeforeCents(balanceBefore);
        txn.setBalanceAfterCents(balanceAfter);
        txn.setTxnDate(paymentDate);
        txn.setReferenceType("RECEIPT");
        txn.setReferenceId(request.getReferenceId());
        txn.setReferenceNo(request.getReferenceNo());
        txn.setPaymentMethod(request.getPaymentMethod());
        txn.setNotes(request.getNotes());
        txn.setStatus("POSTED");
        txn.setCreatedBy(request.getCreatedBy());

        return accountTxnRepository.save(txn);
    }

    @Transactional
    GroupSocietyAccountTxnEntity debitClaim(String groupSocietyId, GroupSocietyClaimDebitRequest request) {
        validateAmount(request.getAmountCents());

        GroupSocietyEntity society = getByIdForUpdate(groupSocietyId);
        validateGroupIsOpenForPosting(society);

        if (request.getClaimId() != null &&
                accountTxnRepository.existsByReferenceTypeAndReferenceIdAndTxnType(
                        "CLAIM",
                        request.getClaimId(),
                        "CLAIM"
                )) {
            throw new RuntimeException("Claim already debited from this group society");
        }

        Long balanceBefore = safeLong(society.getAvailableBalanceCents());
        Long amount = request.getAmountCents();

        if (balanceBefore < amount) {
            throw new RuntimeException(
                    "Insufficient group society balance. Available: "
                            + balanceBefore
                            + ", Required: "
                            + amount
            );
        }

        Long balanceAfter = balanceBefore - amount;
        LocalDate claimDate = request.getClaimDate() != null ? request.getClaimDate() : LocalDate.now();

        society.setAvailableBalanceCents(balanceAfter);
        society.setTotalClaimedCents(safeLong(society.getTotalClaimedCents()) + amount);
        society.setLastClaimDate(claimDate);

        groupSocietyRepository.save(society);

        GroupSocietyAccountTxnEntity txn = new GroupSocietyAccountTxnEntity();
        txn.setGroupSocietyId(groupSocietyId);
        txn.setTxnType("CLAIM");
        txn.setDirection("DEBIT");
        txn.setAmountCents(amount);
        txn.setBalanceBeforeCents(balanceBefore);
        txn.setBalanceAfterCents(balanceAfter);
        txn.setTxnDate(claimDate);
        txn.setReferenceType("CLAIM");
        txn.setReferenceId(request.getClaimId());
        txn.setReferenceNo(request.getClaimNo());
        txn.setNotes(request.getNotes());

        return accountTxnRepository.save(txn);
    }

    public List<GroupSocietyAccountTxnEntity> getStatement(String groupSocietyId, String period) {
        getById(groupSocietyId);

        if (period != null && !period.isBlank()) {
            return accountTxnRepository.findByGroupSocietyIdAndPeriodOrderByTxnDatetimeDesc(groupSocietyId, period);
        }

        return accountTxnRepository.findByGroupSocietyIdOrderByTxnDatetimeDesc(groupSocietyId);
    }

    private GroupSocietyEntity getByIdForUpdate(String id) {
        return groupSocietyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Group society not found: " + id));
    }

    private List<GroupSocietyEntity> enrichPartnerDetails(List<GroupSocietyEntity> societies) {
        Set<String> partnerIds = societies.stream()
                .map(GroupSocietyEntity::getPartnerId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        Map<String, PartnerEntity> partners = partnerRepository.findAllById(partnerIds).stream()
                .collect(Collectors.toMap(PartnerEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        societies.forEach(society -> enrichPartnerDetails(society, partners.get(society.getPartnerId())));
        return societies;
    }

    private GroupSocietyEntity enrichDetails(GroupSocietyEntity society) {
        enrichPartnerDetails(society);
        return enrichProductDetails(society);
    }

    private List<GroupSocietyEntity> enrichProductDetails(List<GroupSocietyEntity> societies) {
        Set<String> productIds = societies.stream()
                .map(GroupSocietyEntity::getProductId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        Map<String, ProductEntity> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        societies.forEach(society -> enrichProductDetails(society, products.get(society.getProductId())));
        return societies;
    }

    private GroupSocietyEntity enrichProductDetails(GroupSocietyEntity society) {
        ProductEntity product = society.getProductId() == null
                ? null
                : productRepository.findById(society.getProductId()).orElse(null);
        return enrichProductDetails(society, product);
    }

    private GroupSocietyEntity enrichProductDetails(GroupSocietyEntity society, ProductEntity product) {
        society.setProductAvailable(product != null);
        society.setProductCode(product == null ? null : product.getCode());
        society.setProductDescription(product == null ? null : product.getDescription());
        return society;
    }

    private PartnerEntity resolveOrCreateGroupPartner(GroupSocietyRequest request) {
        String requestedPartnerId = request.getPartnerId() == null ? "" : request.getPartnerId().trim();
        String groupName = request.getGroupName() == null ? "" : request.getGroupName().trim();

        // partnerId remains accepted for older clients, but the current creation
        // flow supplies groupName and creates the GROUP partner atomically.
        if (!requestedPartnerId.isEmpty()) {
            PartnerEntity existing = partnerRepository.findById(requestedPartnerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "The selected partner does not exist in this tenant: " + requestedPartnerId));
            if (!"GROUP".equalsIgnoreCase(existing.getType())) {
                throw new IllegalArgumentException("The selected partner must be a GROUP partner");
            }
            return existing;
        }

        PartnerInboundDto partnerRequest = new PartnerInboundDto();
        partnerRequest.setPartnerType("GROUP");
        partnerRequest.setName1(groupName.toUpperCase());
        PartnerViewEntity created = partnerServiceV2.create(partnerRequest);
        return partnerRepository.findById(created.getPartnerId())
                .orElseThrow(() -> new IllegalStateException("The group partner was created without a persisted record"));
    }

    private ProductEntity validateGroupSocietyProduct(String productId) {
        ProductEntity product = productRepository.findById(productId.trim())
                .orElseThrow(() -> new IllegalArgumentException("The selected group society product does not exist"));
        String type = product.getType() == null
                ? ""
                : product.getType().trim().toUpperCase().replace('_', '-');
        if (!"GROUP-SOCIETY".equals(type)) {
            throw new IllegalArgumentException("Select a product with product type Group Society");
        }
        return product;
    }

    private GroupSocietyEntity enrichPartnerDetails(GroupSocietyEntity society) {
        PartnerEntity partner = society.getPartnerId() == null
                ? null
                : partnerRepository.findById(society.getPartnerId()).orElse(null);
        return enrichPartnerDetails(society, partner);
    }

    private GroupSocietyEntity enrichPartnerDetails(GroupSocietyEntity society, PartnerEntity partner) {
        society.setPartnerAvailable(partner != null);
        society.setPartnerNumber(partner == null ? null : partner.getNo());
        society.setDisplayName(partnerDisplayName(partner, society.getGroupNo()));
        return society;
    }

    private String partnerDisplayName(PartnerEntity partner, String fallback) {
        if (partner == null) return fallback;
        String name = java.util.stream.Stream.of(partner.getName1(), partner.getName2(), partner.getName3())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "))
                .trim();
        return name.isBlank() ? fallback : name;
    }

    private void validateGroupSocietyRequest(GroupSocietyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Group society details are required");
        }
        boolean hasPartnerId = request.getPartnerId() != null && !request.getPartnerId().isBlank();
        boolean hasGroupName = request.getGroupName() != null && !request.getGroupName().isBlank();
        if (!hasPartnerId && !hasGroupName) {
            throw new IllegalArgumentException("Group / society name is required");
        }
        if (hasPartnerId && hasGroupName) {
            throw new IllegalArgumentException("Supply either an existing group partner or a new group name, not both");
        }
        if (hasGroupName && request.getGroupName().trim().length() > 60) {
            throw new IllegalArgumentException("Group / society name cannot exceed 60 characters");
        }
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            throw new IllegalArgumentException("Group society product is required");
        }
        if (request.getGroupNo() == null || request.getGroupNo().isBlank()) {
            throw new IllegalArgumentException("groupNo is required");
        }
    }

    private void validateGroupIsOpenForPosting(GroupSocietyEntity society) {
        if (!"ACTIVE".equalsIgnoreCase(society.getStatus())) {
            throw new RuntimeException("Cannot post transaction. Group society is not ACTIVE");
        }
    }

    private void validateAmount(Long amountCents) {
        if (amountCents == null || amountCents <= 0) {
            throw new RuntimeException("amountCents must be greater than zero");
        }
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
