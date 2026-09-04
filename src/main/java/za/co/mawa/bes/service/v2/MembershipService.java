package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.dto.v2.sync.MembershipMasterDataDto;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.enums.PremiumStatus;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.PremiumRepository;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.repository.PartnerViewRepository;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;
import za.co.mawa.bes.repository.v2.MembershipMasterDataProjection;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.NumberRangeService;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.mapper.v2.MembershipMapper;
import za.co.mawa.bes.dto.v2.MembershipResponseDto;
import za.co.mawa.bes.utils.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;

@Service(value = "MembershipServiceV2")
public class MembershipService {

    private final MembershipRepository membershipRepository;
    @Autowired
    MembershipPremiumRepository membershipPremiumRepository;
    @Autowired
    NumberRangeService numberRangeService;

    @Autowired
    PremiumRepository premiumRepository;
    @Autowired
    PartnerRepository partnerRepository;
    @Autowired
    PartnerIdentityRepository partnerIdentityRepository;
    @Autowired
    PartnerViewRepository partnerViewRepository;
    @Autowired
    MembershipMapper membershipMapper;

    @Autowired
    MembershipDependentService membershipDependentService;
    @Autowired
    MembershipPlanPremiumRuleService membershipPlanPremiumRuleService;
    @Autowired
    NumberAllocationService numberAllocationService;
    @Autowired
    MembershipPlanService membershipPlanService;
    @Autowired
    MembershipUpdateHandlerRegistry membershipHandlerRegistry;
    @Autowired
    PartnerService partnerService;
    @Autowired
    MembershipChangeService membershipChangeService;
    @Autowired
    MembershipPolicyConfigurationService membershipPolicyConfigurationService;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ApprovalService approvalService;

    @Autowired
    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Page<MembershipEntity> getAllMemberships(String status, Pageable pageable) {
        return getMembershipsByMemberId(null, null, status, pageable);
    }

    public Page<MembershipResponseDto> getAllMembershipResponses(String status, Pageable pageable) {
        return enrichMembershipPage(getAllMemberships(status, pageable));
    }

    public Page<MembershipResponseDto> getMembershipResponsesByMemberId(
            List<String> memberIds,
            String searchQuery,
            String status,
            Pageable pageable
    ) {
        return enrichMembershipPage(getMembershipsByMemberId(memberIds, searchQuery, status, pageable));
    }

    public Page<MembershipEntity> getMembershipsByMemberId(
            List<String> memberIds,
            String searchQuery,
            String status,
            Pageable pageable
    ) {
        String normalizedQuery = searchQuery == null ? "" : searchQuery.trim();
        LinkedHashSet<String> effectiveMemberIds = new LinkedHashSet<>();
        if (memberIds != null) {
            memberIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .forEach(effectiveMemberIds::add);
        }
        if (!normalizedQuery.isEmpty()) {
            partnerViewRepository.findByString(
                            "%" + normalizedQuery + "%",
                            PageRequest.of(0, 500))
                    .stream()
                    .map(partner -> partner.getPartnerId())
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(effectiveMemberIds::add);
        }
        List<String> resolvedMemberIds = List.copyOf(effectiveMemberIds);

        Specification<MembershipEntity> spec = (root, queryObj, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean hasMemberIds = !resolvedMemberIds.isEmpty();
            if (!normalizedQuery.isEmpty()) {
                Predicate membershipNumberMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("membershipNo")),
                        "%" + normalizedQuery.toLowerCase() + "%"
                );
                predicates.add(hasMemberIds
                        ? criteriaBuilder.or(membershipNumberMatch, root.get("memberId").in(resolvedMemberIds))
                        : membershipNumberMatch);
            } else if (hasMemberIds) {
                predicates.add(root.get("memberId").in(resolvedMemberIds));
            }
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("status")),
                        status.trim().toUpperCase()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        membershipChangeService.applyDuePlanChanges(LocalDate.now(), "SYSTEM");
        Page<MembershipEntity> memberships = membershipRepository.findAll(spec, pageable);
        repairMissingPaidUpToPeriods(memberships.getContent());
        return memberships;
    }

    private Page<MembershipResponseDto> enrichMembershipPage(Page<MembershipEntity> page) {
        List<String> memberIds = page.getContent().stream()
                .map(MembershipEntity::getMemberId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        Map<String, PartnerEntity> partnersById = new LinkedHashMap<>();
        if (!memberIds.isEmpty()) {
            partnerRepository.findAllById(memberIds)
                    .forEach(partner -> partnersById.put(partner.getId(), partner));
        }

        Map<String, PartnerIdentityEntity> identitiesByPartner = new LinkedHashMap<>();
        if (!memberIds.isEmpty()) {
            for (PartnerIdentityEntity identity : partnerIdentityRepository.findByPartnerIn(memberIds)) {
                if (identity == null || identity.getPartner() == null
                        || identity.getPartnerIdentityPK() == null) {
                    continue;
                }
                identitiesByPartner.merge(
                        identity.getPartner(),
                        identity,
                        this::preferredIdentity
                );
            }
        }

        return page.map(membership -> {
            MembershipResponseDto response = membershipMapper.toResponse(membership);
            PartnerEntity partner = partnersById.get(membership.getMemberId());
            PartnerIdentityEntity identity = identitiesByPartner.get(membership.getMemberId());

            if (partner != null) {
                response.setMemberNumber(partner.getNo());
                response.setMemberName(formatPartnerName(partner));
            }
            if (identity != null && identity.getPartnerIdentityPK() != null) {
                response.setMemberIdentityType(identity.getPartnerIdentityPK().getType());
                response.setMemberIdentityNumber(identity.getPartnerIdentityPK().getValue());
            }
            return response;
        });
    }

    private PartnerIdentityEntity preferredIdentity(
            PartnerIdentityEntity current,
            PartnerIdentityEntity candidate
    ) {
        return identityPriority(candidate) < identityPriority(current) ? candidate : current;
    }

    private int identityPriority(PartnerIdentityEntity identity) {
        if (identity == null || identity.getPartnerIdentityPK() == null
                || identity.getPartnerIdentityPK().getType() == null) {
            return Integer.MAX_VALUE;
        }
        return switch (identity.getPartnerIdentityPK().getType().trim().toUpperCase()) {
            case "SA-ID", "SA_ID", "RSA-ID", "NATIONAL-ID" -> 0;
            case "PASSPORT" -> 1;
            default -> 2;
        };
    }

    private String formatPartnerName(PartnerEntity partner) {
        if (partner == null) {
            return "";
        }
        String type = partner.getType() == null ? "" : partner.getType().trim().toUpperCase();
        if ("ORGANISATION".equals(type) || "ORGANIZATION".equals(type) || "GROUP".equals(type)) {
            return cleanName(partner.getName1());
        }

        return java.util.stream.Stream.of(
                        partner.getName2(),
                        partner.getName3(),
                        partner.getName1()
                )
                .map(this::cleanName)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private String cleanName(String value) {
        return value == null ? "" : value.trim();
    }

    public Optional<MembershipEntity> getMembershipById(String id) {
        membershipChangeService.synchronizeEffectiveChanges(id, LocalDate.now(), "SYSTEM");
        Optional<MembershipEntity> membership = membershipRepository.findById(id);
        membership.ifPresent(this::repairMissingPaidUpToPeriod);
        return membership;
    }

    private void repairMissingPaidUpToPeriods(List<MembershipEntity> memberships) {
        if (memberships == null || memberships.isEmpty()) return;
        for (MembershipEntity membership : memberships) {
            repairMissingPaidUpToPeriod(membership);
        }
    }

    private void repairMissingPaidUpToPeriod(MembershipEntity membership) {
        if (membership == null || membership.getId() == null) return;
        if (membership.getPaidUpToPeriod() != null && !membership.getPaidUpToPeriod().isBlank()) return;

        List<MembershipPremiumEntity> paidPremiums =
                membershipPremiumRepository.findByMembershipIdInAndStatusOrderByPeriodYYYYMMAsc(
                        membershipIdentifiers(membership),
                        PremiumStatus.PAID
                );
        String calculated = calculateHighestPaidUpToPeriod(paidPremiums);
        if (calculated == null || calculated.isBlank()) return;

        membership.setPaidUpToPeriod(calculated);
        membership.setUpdatedAt(LocalDateTime.now());
        membershipRepository.save(membership);
    }

    @Transactional
    public MembershipEntity createMembership(MembershipEntity membership) {
        try {
            if (membership == null) {
                throw new IllegalArgumentException("Membership details are required");
            }

            String memberId = membership.getMemberId() == null ? "" : membership.getMemberId().trim();
            if (memberId.isEmpty() || !partnerRepository.existsById(memberId)) {
                throw new IllegalArgumentException("The selected member has not synced to the server yet");
            }
            membership.setMemberId(memberId);

            String planId = membership.getPlanId() == null ? "" : membership.getPlanId().trim();
            if (planId.isEmpty()) {
                throw new IllegalArgumentException("Membership plan is required");
            }
            membership.setPlanId(planId);

            String requestedMembershipNo = membership.getMembershipNo() == null
                    ? ""
                    : membership.getMembershipNo().trim();
            if (!requestedMembershipNo.isEmpty()) {
                Optional<MembershipEntity> existing = membershipRepository.findByMembershipNo(requestedMembershipNo);
                if (existing.isPresent()) {
                    MembershipEntity existingMembership = existing.get();
                    if (memberId.equals(existingMembership.getMemberId())
                            && planId.equals(existingMembership.getPlanId())) {
                        return existingMembership;
                    }
                    throw new IllegalStateException(
                            "Membership number " + requestedMembershipNo + " is already in use"
                    );
                }
            }

            long existingMemberships = membershipRepository.countByMemberId(memberId);
            boolean additionalMembership = existingMemberships > 0;
            if (additionalMembership && !membershipPolicyConfigurationService.allowMultipleMemberships()) {
                Optional<MembershipEntity> existingForMember =
                        membershipRepository.findFirstByMemberIdOrderByCreatedAtDesc(memberId);
                if (requestedMembershipNo.isEmpty()
                        && existingForMember.isPresent()
                        && planId.equals(existingForMember.get().getPlanId())
                        && java.util.Objects.equals(
                                membership.getStartDate(),
                                existingForMember.get().getStartDate())) {
                    // A mobile retry can arrive after the original response was
                    // lost, before the device stored the server membership id.
                    return existingForMember.get();
                }
                throw new IllegalStateException("Multiple memberships are not allowed for this member.");
            }

            membership.setCreatedAt(LocalDateTime.ofInstant(java.time.Instant.now(), ZoneId.of("UTC")));
            membership.setCreatedBy(UserContext.getCurrentUserPartner());
            membership.setMembershipNo(requestedMembershipNo.isEmpty()
                    ? numberAllocationService.allocateNumber(TransactionType.MEMBERSHIP)
                    : requestedMembershipNo);
            MembershipPlanEntity selectedPlan = membershipPlanService.getPlanById(planId)
                    .orElseThrow(() -> new IllegalArgumentException("Membership plan not found: " + planId))
                    ;
            membership.setPremiumCents(selectedPlan.getPremiumCents());
            if (membership.getStartDate() == null) {
                throw new IllegalArgumentException("Membership start date is required");
            }
            if (membership.getStatus() == null || membership.getStatus().isBlank()) {
                int waitingMonths = selectedPlan.getWaitingPeriodMonths() == null ? 3 : selectedPlan.getWaitingPeriodMonths();
                membership.setBenefitEligibleFrom(membership.getStartDate().plusMonths(waitingMonths));
                membership.setStatus(waitingMonths > 0
                        ? "WAITING_PERIOD" : "ACTIVE");
            } else {
                membership.setStatus(membership.getStatus().trim().toUpperCase());
                if (membership.getBenefitEligibleFrom() == null) {
                    int waitingMonths = selectedPlan.getWaitingPeriodMonths() == null ? 3 : selectedPlan.getWaitingPeriodMonths();
                    membership.setBenefitEligibleFrom(membership.getStartDate().plusMonths(waitingMonths));
                }
            }

            boolean approvalRequired = additionalMembership
                    && membershipPolicyConfigurationService.additionalMembershipRequiresApproval();
            if (approvalRequired) {
                membership.setStatus("PENDING_APPROVAL");
            } else if (additionalMembership) {
                membership.setStatus("ACTIVE");
            }

            MembershipEntity savedMembership = membershipRepository.save(membership);
            membershipChangeService.ensureBaselineHistory(savedMembership, UserContext.getCurrentUserId());
            partnerService.addRole(savedMembership.getMemberId(), "MEMBER");

            if (approvalRequired) {
                za.co.mawa.bes.dto.v2.ApprovalSubmitRequest approval = new za.co.mawa.bes.dto.v2.ApprovalSubmitRequest();
                approval.setApprovalType(za.co.mawa.bes.enums.ApprovalType.ADDITIONAL_MEMBERSHIP);
                approval.setReferenceId(savedMembership.getId());
                approval.setReferenceNo(savedMembership.getMembershipNo());
                PartnerEntity member = partnerRepository.findById(savedMembership.getMemberId()).orElse(null);
                String memberName = formatPartnerName(member);
                approval.setTitle("Additional membership - " + savedMembership.getMembershipNo()
                        + " - " + (memberName.isBlank() ? savedMembership.getMemberId() : memberName));
                approval.setDescription("Review the member and existing memberships before approving this additional membership.");
                approval.setRequesterId(UserContext.getCurrentUserId());
                Map<String, Object> approvalPayload = new LinkedHashMap<>();
                approvalPayload.put("membershipNumber", savedMembership.getMembershipNo());
                approvalPayload.put("memberName", memberName);
                approvalPayload.put("membershipStatus", savedMembership.getStatus());
                approvalPayload.put("planId", savedMembership.getPlanId());
                approvalPayload.put("membershipId", savedMembership.getId());
                approvalPayload.put("memberId", savedMembership.getMemberId());
                approvalPayload.put("attachmentObjectIds", List.of(savedMembership.getId(), savedMembership.getMemberId()));
                approval.setPayloadJson(objectMapper.writeValueAsString(approvalPayload));
                var approvalResponse = approvalService.submitForApproval(approval);
                savedMembership.setApprovalRequestId(approvalResponse.getId());
                savedMembership.setUpdatedBy(UserContext.getCurrentUserId());
                savedMembership.setUpdatedAt(LocalDateTime.now());
                savedMembership = membershipRepository.save(savedMembership);
            }
            return savedMembership;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create membership", e);
        }
    }

    public List<MembershipMasterDataDto> getMasterData(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(50, Math.min(size, 500));

        return membershipRepository.findMasterData(PageRequest.of(safePage, safeSize))
                .stream()
                .map(this::toMasterDataDto)
                .toList();
    }

    private MembershipMasterDataDto toMasterDataDto(MembershipMasterDataProjection row) {
        return MembershipMasterDataDto.builder()
                .membershipId(row.getMembershipId())
                .membershipNo(row.getMembershipNo())
                .partnerId(row.getPartnerId())
                .planId(row.getPlanId())
                .premiumCents(row.getPremiumCents())
                .startDate(row.getStartDate())
                .joinDate(row.getJoinDate())
                .membershipStatus(row.getMembershipStatus())
                .paidUpToPeriod(row.getPaidUpToPeriod())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .partnerNo(row.getPartnerNo())
                .partnerType(row.getPartnerType())
                .name1(row.getName1())
                .name2(row.getName2())
                .name3(row.getName3())
                .identityType(row.getIdentityType())
                .identityNumber(row.getIdentityNumber())
                .birthDate(row.getBirthDate())
                .gender(row.getGender())
                .partnerStatus(row.getPartnerStatus())
                .build();
    }

    @Transactional
    public Optional<MembershipEntity> updateMembership(String id, MembershipEntity membership) {
        membershipRepository.findById(id)
                .map(existingMembership -> {
                    if (membership.getMemberId() != null && !membership.getMemberId().equals(existingMembership.getMemberId())) {
                        throw new IllegalArgumentException("Membership holder changes require an approved transfer request");
                    }
                    if (membership.getPlanId() != null && !membership.getPlanId().equals(existingMembership.getPlanId())) {
                        throw new IllegalArgumentException("Membership plan changes require an approved plan-change request");
                    }
                    if (membership.getStatus() != null
                            && !membership.getStatus().equalsIgnoreCase(existingMembership.getStatus())) {
                        throw new IllegalArgumentException(
                                "Membership status changes require an approved Reactivate, Deactivate, Suspend or Cancel action");
                    }
                    existingMembership.setMembershipNo(membership.getMembershipNo());
                    existingMembership.setStartDate(membership.getStartDate());
                    existingMembership.setEndDate(membership.getEndDate());
                    // Membership status is changed only by the approval-backed status action flow.
                    // Paid Up To is derived exclusively from PAID premium rows.
                    existingMembership.setJoinDate(membership.getJoinDate());
                    MembershipEntity savedMembership = membershipRepository.save(existingMembership);
                    partnerService.addRole(savedMembership.getMemberId(), "MEMBER");
                    return savedMembership;
                });
        recalculatePaidUpToPeriod(id);
        membershipHandlerRegistry.handleUpdate(id);
        return membershipRepository.findById(id);
    }

    public boolean deleteMembership(String id) {
        if (membershipRepository.existsById(id)) {
            membershipRepository.deleteById(id);
            return true;
        }
        return false;
    }
    @Transactional
    public String recalculatePaidUpToPeriod(String membershipId) {
        MembershipEntity membership = resolveMembership(membershipId);

        // Premium writes and paid-up-to recalculation often happen inside the same
        // offline-sync transaction. Force pending premium changes to the database
        // before querying PAID rows so the membership never keeps the old period.
        membershipPremiumRepository.flush();

        List<MembershipPremiumEntity> paidPremiums =
                membershipPremiumRepository.findByMembershipIdInAndStatusOrderByPeriodYYYYMMAsc(
                        membershipIdentifiers(membership),
                        PremiumStatus.PAID
                );

        String paidUpToPeriod = calculateHighestPaidUpToPeriod(paidPremiums);

        membership.setPaidUpToPeriod(paidUpToPeriod);
        membership.setUpdatedAt(LocalDateTime.now());

        membershipRepository.saveAndFlush(membership);

        return paidUpToPeriod;
    }

    public MembershipEntity resolveMembership(String membershipId) {
        if (membershipId == null || membershipId.isBlank()) {
            throw new RuntimeException("Membership id is required");
        }

        MembershipEntity resolved = membershipRepository.findById(membershipId)
                .or(() -> membershipRepository.findByOldId(membershipId))
                .orElseThrow(() -> new RuntimeException("Membership not found: " + membershipId));
        membershipChangeService.synchronizeEffectiveChanges(resolved.getId(), LocalDate.now(), "SYSTEM");
        resolved = membershipRepository.findById(resolved.getId()).orElse(resolved);

        Set<String> visited = new LinkedHashSet<>();
        while (resolved.getMergedIntoMembershipId() != null
                && !resolved.getMergedIntoMembershipId().isBlank()) {
            if (!visited.add(resolved.getId())) {
                throw new IllegalStateException("Circular merged membership reference detected");
            }
            String primaryId = resolved.getMergedIntoMembershipId();
            resolved = membershipRepository.findById(primaryId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Primary membership for merged membership was not found: " + primaryId));
        }
        return resolved;
    }

    public List<String> membershipIdentifiers(String membershipId) {
        return membershipIdentifiers(resolveMembership(membershipId));
    }

    private List<String> membershipIdentifiers(MembershipEntity membership) {
        Set<String> identifiers = new LinkedHashSet<>();
        if (membership.getId() != null && !membership.getId().isBlank()) {
            identifiers.add(membership.getId());
        }
        if (membership.getOldId() != null && !membership.getOldId().isBlank()) {
            identifiers.add(membership.getOldId());
        }
        for (MembershipEntity alias : membershipRepository.findByMergedIntoMembershipId(membership.getId())) {
            if (alias.getId() != null && !alias.getId().isBlank()) {
                identifiers.add(alias.getId());
            }
            if (alias.getOldId() != null && !alias.getOldId().isBlank()) {
                identifiers.add(alias.getOldId());
            }
        }
        return List.copyOf(identifiers);
    }

    private String calculateHighestPaidUpToPeriod(List<MembershipPremiumEntity> paidPremiums) {
        if (paidPremiums == null || paidPremiums.isEmpty()) {
            return null;
        }

        return paidPremiums.stream()
                .map(MembershipPremiumEntity::getPeriodYYYYMM)
                .filter(PeriodUtil::isValidPeriod)
                .max(String::compareTo)
                .orElse(null);
    }

}
