package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.membership.change.*;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.*;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.*;
import za.co.mawa.bes.service.PartnerService;
import za.co.mawa.bes.service.SettingService;
import za.co.mawa.bes.utils.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MembershipChangeService {
    private static final String CONFIG_ID = "DEFAULT";
    private static final int DEFAULT_WAITING_PERIOD_MONTHS = 3;

    private final MembershipRepository membershipRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipChangeConfigurationRepository configurationRepository;
    private final MembershipChangeRequestRepository changeRequestRepository;
    private final MembershipPlanHistoryRepository planHistoryRepository;
    private final MembershipPremiumPlanHistoryRepository premiumPlanHistoryRepository;
    private final MembershipChangeAuditRepository auditRepository;
    private final PartnerRepository partnerRepository;
    private final MembershipDependentRepository membershipDependentRepository;
    private final MembershipPlanPremiumRuleService membershipPlanPremiumRuleService;
    private final MembershipUpdateHandlerRegistry membershipUpdateHandlerRegistry;
    private final PartnerService partnerService;
    private final SettingService settingService;
    private final ObjectProvider<ApprovalService> approvalServiceProvider;
    private final ObjectMapper objectMapper;
    private final MembershipActionGuardService membershipActionGuardService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public MembershipChangeConfigurationDto getConfiguration() {
        MembershipChangeConfigurationEntity entity = configurationRepository.findById(CONFIG_ID)
                .orElseGet(() -> MembershipChangeConfigurationEntity.builder()
                        .id(CONFIG_ID)
                        .planChangeWaitingPeriodMonths(DEFAULT_WAITING_PERIOD_MONTHS)
                        .build());
        return toConfigurationDto(entity);
    }

    @Transactional
    public MembershipChangeConfigurationDto updateConfiguration(MembershipChangeConfigurationDto request, String actor) {
        if (request == null || request.getPlanChangeWaitingPeriodMonths() == null) {
            throw new IllegalArgumentException("Plan-change waiting period is required");
        }
        if (request.getPlanChangeWaitingPeriodMonths() < 0 || request.getPlanChangeWaitingPeriodMonths() > 120) {
            throw new IllegalArgumentException("Plan-change waiting period must be between 0 and 120 months");
        }
        MembershipChangeConfigurationEntity entity = configurationRepository.findById(CONFIG_ID)
                .orElseGet(() -> MembershipChangeConfigurationEntity.builder().id(CONFIG_ID).build());
        entity.setPlanChangeWaitingPeriodMonths(request.getPlanChangeWaitingPeriodMonths());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(actor(actor));
        return toConfigurationDto(configurationRepository.save(entity));
    }

    @Transactional
    public MembershipChangeResponse requestTransfer(String membershipId, MembershipTransferRequest request, String actor) {
        MembershipEntity membership = getMembershipForUpdate(membershipId);
        membershipActionGuardService.requireActionable(membership);
        requireNoOpenChange(membershipId);
        String newMemberId = clean(request == null ? null : request.getNewMemberId());
        if (newMemberId == null) throw new IllegalArgumentException("New member is required");
        if (newMemberId.equals(membership.getMemberId())) throw new IllegalArgumentException("The selected partner is already the membership holder");
        if (!partnerRepository.existsById(newMemberId)) throw new IllegalArgumentException("New member partner was not found: " + newMemberId);
        if (!membershipDependentRepository.existsByMembershipIdAndDependentPartnerIdAndStatus(
                membershipId, newMemberId, MembershipDependentStatus.ACTIVE)) {
            throw new IllegalArgumentException(
                    "Membership can only be transferred to an active dependent on the same membership");
        }

        String reason = requireReason(request.getReason());
        String actionBy = actor(actor);
        MembershipChangeRequestEntity change = MembershipChangeRequestEntity.builder()
                .membershipId(membership.getId())
                .changeType(MembershipChangeType.TRANSFER)
                .status(MembershipChangeStatus.PENDING_APPROVAL)
                .oldMemberId(membership.getMemberId())
                .newMemberId(newMemberId)
                .oldPlanId(membership.getPlanId())
                .newPlanId(membership.getPlanId())
                .waitingPeriodMonths(0)
                .reason(reason)
                .requestedAt(LocalDateTime.now())
                .requestedBy(actionBy)
                .updatedAt(LocalDateTime.now())
                .updatedBy(actionBy)
                .build();
        change = changeRequestRepository.save(change);
        audit(change, "REQUESTED", Map.of("memberId", membership.getMemberId()), Map.of("memberId", newMemberId), change.getReason(), actionBy);
        ApprovalRequestResponse approval = submit(change, membership, ApprovalType.MEMBERSHIP_TRANSFER, actionBy);
        change.setApprovalRequestId(approval.getId());
        change.setUpdatedAt(LocalDateTime.now());
        return toResponse(changeRequestRepository.save(change));
    }

    @Transactional
    public MembershipChangeResponse requestPlanChange(String membershipId, MembershipPlanChangeRequest request, String actor) {
        MembershipEntity membership = getMembershipForUpdate(membershipId);
        membershipActionGuardService.requireActionable(membership);
        synchronizeEffectiveChanges(membership.getId(), LocalDate.now(), actor(actor));
        membership = getMembershipForUpdate(membershipId);
        requireNoOpenChange(membershipId);
        String newPlanId = clean(request == null ? null : request.getNewPlanId());
        if (newPlanId == null) throw new IllegalArgumentException("New membership plan is required");
        if (newPlanId.equals(membership.getPlanId())) throw new IllegalArgumentException("The selected plan is already active on the membership");
        MembershipPlanEntity newPlan = membershipPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new IllegalArgumentException("Membership plan not found: " + newPlanId));
        if (!Boolean.TRUE.equals(newPlan.getActive())) throw new IllegalArgumentException("The selected membership plan is inactive");

        String reason = requireReason(request.getReason());
        String actionBy = actor(actor);
        int waitingMonths = configurationRepository.findById(CONFIG_ID)
                .map(MembershipChangeConfigurationEntity::getPlanChangeWaitingPeriodMonths)
                .orElse(DEFAULT_WAITING_PERIOD_MONTHS);
        MembershipChangeRequestEntity change = MembershipChangeRequestEntity.builder()
                .membershipId(membership.getId())
                .changeType(MembershipChangeType.PLAN_CHANGE)
                .status(MembershipChangeStatus.PENDING_APPROVAL)
                .oldMemberId(membership.getMemberId())
                .newMemberId(membership.getMemberId())
                .oldPlanId(membership.getPlanId())
                .newPlanId(newPlanId)
                .waitingPeriodMonths(waitingMonths)
                .reason(reason)
                .requestedAt(LocalDateTime.now())
                .requestedBy(actionBy)
                .updatedAt(LocalDateTime.now())
                .updatedBy(actionBy)
                .build();
        change = changeRequestRepository.save(change);
        audit(change, "REQUESTED", Map.of("planId", membership.getPlanId()), Map.of("planId", newPlanId, "waitingPeriodMonths", waitingMonths), change.getReason(), actionBy);
        ApprovalRequestResponse approval = submit(change, membership, ApprovalType.MEMBERSHIP_PLAN_CHANGE, actionBy);
        change.setApprovalRequestId(approval.getId());
        change.setUpdatedAt(LocalDateTime.now());
        return toResponse(changeRequestRepository.save(change));
    }

    @Transactional
    public MembershipChangeResponse requestMerge(String primaryMembershipId, MembershipMergeRequest request, String actor) {
        MembershipEntity primary = getMembershipForUpdate(primaryMembershipId);
        membershipActionGuardService.requireActionable(primary);
        String sourceId = clean(request == null ? null : request.getSourceMembershipId());
        if (sourceId == null) throw new IllegalArgumentException("Source membership is required");
        if (sourceId.equals(primaryMembershipId)) throw new IllegalArgumentException("A membership cannot be merged into itself");
        MembershipEntity source = getMembershipForUpdate(sourceId);
        membershipActionGuardService.requireActionable(source);
        if ("MERGED".equalsIgnoreCase(primary.getStatus()) || "MERGED".equalsIgnoreCase(source.getStatus())) {
            throw new IllegalArgumentException("A membership that has already been merged cannot be used in another merge");
        }
        requireNoOpenChange(primaryMembershipId);
        requireNoOpenChange(sourceId);
        requireNoPendingMergeBlockers(primaryMembershipId);
        requireNoPendingMergeBlockers(sourceId);

        String actionBy = actor(actor);
        MembershipChangeRequestEntity change = MembershipChangeRequestEntity.builder()
                .membershipId(primaryMembershipId)
                .sourceMembershipId(sourceId)
                .changeType(MembershipChangeType.MERGE)
                .status(MembershipChangeStatus.PENDING_APPROVAL)
                .oldMemberId(source.getMemberId())
                .newMemberId(primary.getMemberId())
                .oldPlanId(source.getPlanId())
                .newPlanId(primary.getPlanId())
                .effectiveDate(LocalDate.now())
                .reason(requireReason(request == null ? null : request.getReason()))
                .requestedAt(LocalDateTime.now()).requestedBy(actionBy)
                .updatedAt(LocalDateTime.now()).updatedBy(actionBy).build();
        change = changeRequestRepository.save(change);
        audit(change, "REQUESTED",
                Map.of("membershipId", sourceId, "membershipNumber", source.getMembershipNo()),
                Map.of("membershipId", primaryMembershipId, "membershipNumber", primary.getMembershipNo()),
                change.getReason(), actionBy);
        ApprovalRequestResponse approval = submit(change, primary, ApprovalType.MEMBERSHIP_MERGE, actionBy);
        change.setApprovalRequestId(approval.getId());
        return toResponse(changeRequestRepository.save(change));
    }


    @Transactional
    public MembershipChangeResponse requestDependentAdd(
            String membershipId,
            MembershipDependentAddRequest request,
            String actor
    ) {
        MembershipEntity membership = getMembershipForUpdate(membershipId);
        membershipActionGuardService.requireActionable(membership);
        String partnerId = clean(request == null ? null : request.getDependentPartnerId());
        if (partnerId == null) throw new IllegalArgumentException("Dependent is required");
        DependentType dependentType = request == null ? null : request.getDependentType();
        if (dependentType == null || dependentType == DependentType.ANY || dependentType == DependentType.MAIN_MEMBER) {
            throw new IllegalArgumentException("A valid dependent relationship is required");
        }
        validateDependentPartner(membership, partnerId, null);
        requireNoConflictingDependentChange(membershipId, null, partnerId);
        if (membershipDependentRepository.existsVisibleByMembershipIdAndPartnerId(
                membershipId, partnerId,
                Set.of(MembershipDependentStatus.ACTIVE, MembershipDependentStatus.DECEASED))) {
            throw new IllegalArgumentException("The selected person is already linked to this membership");
        }

        String actionBy = actor(actor);
        MembershipChangeRequestEntity change = MembershipChangeRequestEntity.builder()
                .membershipId(membershipId)
                .changeType(MembershipChangeType.ADD_DEPENDENT)
                .status(MembershipChangeStatus.PENDING_APPROVAL)
                .oldMemberId(membership.getMemberId())
                .newMemberId(membership.getMemberId())
                .oldPlanId(membership.getPlanId())
                .newPlanId(membership.getPlanId())
                .newDependentPartnerId(partnerId)
                .newDependentType(dependentType.name())
                .waitingPeriodMonths(0)
                .effectiveDate(LocalDate.now())
                .reason(requireReason(request == null ? null : request.getReason()))
                .requestedAt(LocalDateTime.now())
                .requestedBy(actionBy)
                .updatedAt(LocalDateTime.now())
                .updatedBy(actionBy)
                .build();
        change = changeRequestRepository.save(change);
        audit(change, "REQUESTED", null,
                Map.of("dependentPartnerId", partnerId, "dependentType", dependentType.name()),
                change.getReason(), actionBy);
        return submitOrApplyDependentChange(change, membership, actionBy);
    }

    @Transactional
    public MembershipChangeResponse requestDependentRemove(
            String membershipId,
            String dependentId,
            MembershipDependentRemoveRequest request,
            String actor
    ) {
        MembershipEntity membership = getMembershipForUpdate(membershipId);
        membershipActionGuardService.requireActionable(membership);
        MembershipDependentEntity existing = getVisibleDependent(membershipId, dependentId);
        if (existing.getStatus() == MembershipDependentStatus.DECEASED) {
            throw new IllegalArgumentException("A deceased dependent cannot be removed from membership history");
        }
        requireNoConflictingDependentChange(membershipId, existing.getId(), null);

        String actionBy = actor(actor);
        MembershipChangeRequestEntity change = MembershipChangeRequestEntity.builder()
                .membershipId(membershipId)
                .changeType(MembershipChangeType.REMOVE_DEPENDENT)
                .status(MembershipChangeStatus.PENDING_APPROVAL)
                .oldMemberId(membership.getMemberId())
                .newMemberId(membership.getMemberId())
                .oldPlanId(membership.getPlanId())
                .newPlanId(membership.getPlanId())
                .oldDependentId(existing.getId())
                .oldDependentPartnerId(existing.getDependentPartnerId())
                .oldDependentType(existing.getDependentType() == null ? null : existing.getDependentType().name())
                .waitingPeriodMonths(0)
                .effectiveDate(LocalDate.now())
                .reason(requireReason(request == null ? null : request.getReason()))
                .requestedAt(LocalDateTime.now())
                .requestedBy(actionBy)
                .updatedAt(LocalDateTime.now())
                .updatedBy(actionBy)
                .build();
        change = changeRequestRepository.save(change);
        audit(change, "REQUESTED",
                Map.of("dependentId", existing.getId(),
                        "dependentPartnerId", existing.getDependentPartnerId(),
                        "dependentType", existing.getDependentType().name()),
                null, change.getReason(), actionBy);
        return submitOrApplyDependentChange(change, membership, actionBy);
    }

    @Transactional
    public MembershipChangeResponse requestDependentReplace(
            String membershipId,
            String dependentId,
            MembershipDependentReplaceRequest request,
            String actor
    ) {
        MembershipEntity membership = getMembershipForUpdate(membershipId);
        membershipActionGuardService.requireActionable(membership);
        MembershipDependentEntity existing = getVisibleDependent(membershipId, dependentId);
        if (existing.getStatus() == MembershipDependentStatus.DECEASED) {
            throw new IllegalArgumentException("A deceased dependent cannot be replaced");
        }

        String partnerId = clean(request == null ? null : request.getDependentPartnerId());
        if (partnerId == null) throw new IllegalArgumentException("Replacement dependent is required");
        DependentType dependentType = request == null ? null : request.getDependentType();
        if (dependentType == null || dependentType == DependentType.ANY || dependentType == DependentType.MAIN_MEMBER) {
            throw new IllegalArgumentException("A valid replacement relationship is required");
        }
        if (partnerId.equals(existing.getDependentPartnerId())) {
            throw new IllegalArgumentException("Replacement dependent must be a different person");
        }
        validateDependentPartner(membership, partnerId, existing.getDependentPartnerId());
        requireNoConflictingDependentChange(membershipId, existing.getId(), partnerId);
        if (membershipDependentRepository.existsVisibleByMembershipIdAndPartnerId(
                        membershipId, partnerId,
                        Set.of(MembershipDependentStatus.ACTIVE, MembershipDependentStatus.DECEASED))) {
            throw new IllegalArgumentException("The replacement person is already linked to this membership");
        }

        String actionBy = actor(actor);
        MembershipChangeRequestEntity change = MembershipChangeRequestEntity.builder()
                .membershipId(membershipId)
                .changeType(MembershipChangeType.REPLACE_DEPENDENT)
                .status(MembershipChangeStatus.PENDING_APPROVAL)
                .oldMemberId(membership.getMemberId())
                .newMemberId(membership.getMemberId())
                .oldPlanId(membership.getPlanId())
                .newPlanId(membership.getPlanId())
                .oldDependentId(existing.getId())
                .oldDependentPartnerId(existing.getDependentPartnerId())
                .newDependentPartnerId(partnerId)
                .oldDependentType(existing.getDependentType() == null ? null : existing.getDependentType().name())
                .newDependentType(dependentType.name())
                .waitingPeriodMonths(0)
                .effectiveDate(LocalDate.now())
                .reason(requireReason(request == null ? null : request.getReason()))
                .requestedAt(LocalDateTime.now())
                .requestedBy(actionBy)
                .updatedAt(LocalDateTime.now())
                .updatedBy(actionBy)
                .build();
        change = changeRequestRepository.save(change);
        audit(change, "REQUESTED",
                Map.of("dependentId", existing.getId(),
                        "dependentPartnerId", existing.getDependentPartnerId(),
                        "dependentType", existing.getDependentType().name()),
                Map.of("dependentPartnerId", partnerId, "dependentType", dependentType.name()),
                change.getReason(), actionBy);
        return submitOrApplyDependentChange(change, membership, actionBy);
    }

    @Transactional(readOnly = true)
    public List<MembershipChangeResponse> listChanges(String membershipId) {
        return changeRequestRepository.findByMembershipIdOrderByRequestedAtDesc(membershipId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MembershipChangeAuditResponse> listAudit(String membershipId) {
        return auditRepository.findByMembershipIdOrderByPerformedAtDesc(membershipId).stream().map(this::toAuditResponse).toList();
    }

    @Transactional
    public void approved(String changeRequestId, String actionBy) {
        MembershipChangeRequestEntity change = getChange(changeRequestId);
        if (change.getStatus() != MembershipChangeStatus.PENDING_APPROVAL) return;
        membershipActionGuardService.requireActionable(change.getMembershipId());
        String actor = actor(actionBy);
        change.setApprovedAt(LocalDateTime.now());
        change.setApprovedBy(actor);
        change.setUpdatedAt(LocalDateTime.now());
        change.setUpdatedBy(actor);
        if (change.getChangeType() == MembershipChangeType.MERGE) {
            change.setEffectiveDate(LocalDate.now());
            changeRequestRepository.save(change);
            applyMerge(change, actor);
        } else if (change.getChangeType() == MembershipChangeType.TRANSFER) {
            change.setEffectiveDate(LocalDate.now());
            changeRequestRepository.save(change);
            audit(change, "APPROVED",
                    Map.of("memberId", change.getOldMemberId()),
                    Map.of("memberId", change.getNewMemberId(), "effectiveDate", change.getEffectiveDate().toString()),
                    "Membership transfer approved",
                    actor);
            applyTransfer(change, actor);
        } else if (isDependentChange(change.getChangeType())) {
            change.setEffectiveDate(LocalDate.now());
            changeRequestRepository.save(change);
            audit(change, "APPROVED", dependentOldValue(change), dependentNewValue(change),
                    "Membership dependent change approved", actor);
            applyDependentChange(change, actor);
        } else {
            MembershipEntity membership = getMembershipForUpdate(change.getMembershipId());
            MembershipPlanEntity newPlan = membershipPlanRepository.findById(change.getNewPlanId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Approved membership plan no longer exists: " + change.getNewPlanId()));
            LocalDate today = LocalDate.now();
            ensureBaselineHistory(membership, actor);
            ensureBaselinePremiumHistory(membership, actor);

            long currentPremium = membership.getPremiumCents() == null ? 0L : membership.getPremiumCents();
            long newPremium = calculateTotalPremiumCents(membership.getId(), newPlan, today);
            boolean downgrade = isPlanDowngrade(change.getOldPlanId(), newPlan);

            applyPremiumPlanChange(change, membership, newPlan, today, actor);

            int waitingMonths = downgrade
                    ? 0
                    : (change.getWaitingPeriodMonths() == null
                        ? DEFAULT_WAITING_PERIOD_MONTHS : change.getWaitingPeriodMonths());
            change.setWaitingPeriodMonths(waitingMonths);
            change.setEffectiveDate(today.plusMonths(waitingMonths));
            change.setStatus(MembershipChangeStatus.APPROVED_SCHEDULED);
            change.setUpdatedAt(LocalDateTime.now());
            change.setUpdatedBy(actor);
            changeRequestRepository.save(change);

            Map<String, Object> approvedValues = new LinkedHashMap<>();
            approvedValues.put("planId", change.getNewPlanId());
            approvedValues.put("premiumCents", newPremium);
            approvedValues.put("premiumEffectiveDate", today.toString());
            approvedValues.put("benefitEffectiveDate", change.getEffectiveDate().toString());
            approvedValues.put("changeDirection", downgrade ? "DOWNGRADE" : "UPGRADE");
            audit(change, "APPROVED",
                    Map.of("planId", change.getOldPlanId(), "premiumCents", currentPremium),
                    approvedValues,
                    downgrade
                            ? "Membership downgrade approved; premium and benefits are effective immediately"
                            : "Membership upgrade approved; premium is effective immediately and upgraded benefits are waiting-period controlled",
                    actor);

            if (!change.getEffectiveDate().isAfter(today)) applyPlanChange(change, actor);
        }
    }

    private void applyMerge(MembershipChangeRequestEntity change, String actor) {
        MembershipEntity primary = getMembershipForUpdate(change.getMembershipId());
        MembershipEntity source = getMembershipForUpdate(change.getSourceMembershipId());
        if ("MERGED".equalsIgnoreCase(source.getStatus())) {
            if (primary.getId().equals(source.getMergedIntoMembershipId())) return;
            throw new IllegalStateException("Source membership has already been merged elsewhere");
        }

        // Move non-conflicting dependants. Duplicate people remain on the source as
        // historical rows and are visible through the permanent merge relationship.
        jdbcTemplate.update("""
            UPDATE membership_dependent s
            LEFT JOIN membership_dependent p
              ON p.membership_id = ?
             AND p.dependent_partner_id = s.dependent_partner_id
             AND p.status IN ('ACTIVE','DECEASED')
               SET s.membership_id = ?
             WHERE s.membership_id = ? AND p.id IS NULL
            """, primary.getId(), primary.getId(), source.getId());

        // Preserve one obligation per period. Non-conflicting premiums move to the
        // primary membership; conflicting periods remain immutable on the source.
        jdbcTemplate.update("""
            UPDATE membership_premium s
            LEFT JOIN membership_premium p
              ON p.membership_id = ? AND p.period_yyyymm = s.period_yyyymm
               SET s.membership_id = ?
             WHERE s.membership_id = ? AND p.id IS NULL
            """, primary.getId(), primary.getId(), source.getId());

        updateMembershipReference("payment_batch", primary.getId(), source.getId());
        updateMembershipReference("receipt", primary.getId(), source.getId());
        updateMembershipReference("receipt_allocation", primary.getId(), source.getId());
        updateMembershipReference("manual_premium_receipt", primary.getId(), source.getId());
        updateMembershipReference("membership_claim", primary.getId(), source.getId());

        String paidUpTo = jdbcTemplate.queryForObject("""
            SELECT MAX(period_yyyymm) FROM membership_premium
             WHERE membership_id IN (?, ?) AND status = 'PAID'
            """, String.class, primary.getId(), source.getId());
        primary.setPaidUpToPeriod(paidUpTo);
        primary.setUpdatedAt(LocalDateTime.now());
        primary.setUpdatedBy(actor);
        membershipRepository.save(primary);

        source.setStatus("MERGED");
        source.setEndDate(LocalDate.now());
        source.setMergedIntoMembershipId(primary.getId());
        source.setMergedAt(LocalDateTime.now());
        source.setMergedBy(actor);
        source.setUpdatedAt(LocalDateTime.now());
        source.setUpdatedBy(actor);
        membershipRepository.save(source);

        change.setStatus(MembershipChangeStatus.APPLIED);
        change.setAppliedAt(LocalDateTime.now());
        change.setAppliedBy(actor);
        change.setUpdatedAt(LocalDateTime.now());
        change.setUpdatedBy(actor);
        changeRequestRepository.save(change);
        audit(change, "APPLIED",
                Map.of("sourceMembershipId", source.getId(), "sourceMembershipNumber", source.getMembershipNo()),
                Map.of("primaryMembershipId", primary.getId(), "primaryMembershipNumber", primary.getMembershipNo(),
                       "paidUpToPeriod", paidUpTo == null ? "" : paidUpTo),
                "Membership merge applied", actor);
        membershipUpdateHandlerRegistry.handleUpdate(primary.getId());
    }

    private void updateMembershipReference(String table, String primaryId, String sourceId) {
        Set<String> allowed = Set.of("payment_batch", "receipt", "receipt_allocation",
                "manual_premium_receipt", "membership_claim");
        if (!allowed.contains(table)) throw new IllegalArgumentException("Unsupported membership reference table");
        jdbcTemplate.update("UPDATE " + table + " SET membership_id = ? WHERE membership_id = ?", primaryId, sourceId);
    }

    @Transactional
    public void rejected(String changeRequestId, String actionBy, boolean cancelled) {
        MembershipChangeRequestEntity change = getChange(changeRequestId);
        if (Set.of(MembershipChangeStatus.APPLIED, MembershipChangeStatus.REJECTED, MembershipChangeStatus.CANCELLED).contains(change.getStatus())) return;
        change.setStatus(cancelled ? MembershipChangeStatus.CANCELLED : MembershipChangeStatus.REJECTED);
        change.setUpdatedAt(LocalDateTime.now());
        change.setUpdatedBy(actor(actionBy));
        changeRequestRepository.save(change);
        audit(change, cancelled ? "CANCELLED" : "REJECTED", null, null, cancelled ? "Membership change cancelled" : "Membership change rejected", actor(actionBy));
    }

    @Transactional
    public void synchronizeEffectiveChanges(String membershipId, LocalDate asAt, String actor) {
        List<MembershipChangeRequestEntity> due = changeRequestRepository.findDueForMembershipForUpdate(
                membershipId,
                MembershipChangeStatus.APPROVED_SCHEDULED,
                asAt == null ? LocalDate.now() : asAt);
        for (MembershipChangeRequestEntity change : due) applyPlanChange(change, actor(actor));
    }

    @Transactional
    public int applyDuePlanChanges(LocalDate asAt, String actor) {
        List<MembershipChangeRequestEntity> due = changeRequestRepository.findDueForUpdate(
                MembershipChangeStatus.APPROVED_SCHEDULED,
                asAt == null ? LocalDate.now() : asAt);
        for (MembershipChangeRequestEntity change : due) {
            applyPlanChange(change, actor(actor));
        }
        return due.size();
    }

    @Transactional
    public String resolveCoveragePlanId(String membershipId, LocalDate eventDate, String actor) {
        MembershipEntity membership = getMembership(membershipId);
        LocalDate date = eventDate == null ? LocalDate.now() : eventDate;
        synchronizeEffectiveChanges(membershipId, LocalDate.now(), actor);
        ensureBaselineHistory(membership, actor(actor));
        return planHistoryRepository.findEffective(membershipId, date).stream()
                .findFirst().map(MembershipPlanHistoryEntity::getPlanId).orElse(membership.getPlanId());
    }

    @Transactional
    public void ensureBaselineHistory(MembershipEntity membership, String actor) {
        if (!planHistoryRepository.findByMembershipIdOrderByEffectiveFromAsc(membership.getId()).isEmpty()) return;
        LocalDate from = membership.getStartDate() != null ? membership.getStartDate()
                : membership.getJoinDate() != null ? membership.getJoinDate() : LocalDate.now();
        planHistoryRepository.save(MembershipPlanHistoryEntity.builder()
                .membershipId(membership.getId()).planId(membership.getPlanId()).effectiveFrom(from)
                .createdAt(LocalDateTime.now()).createdBy(actor(actor)).build());
    }

    private void ensureBaselinePremiumHistory(MembershipEntity membership, String actor) {
        if (!premiumPlanHistoryRepository.findByMembershipIdOrderByEffectiveFromAsc(membership.getId()).isEmpty()) return;
        LocalDate from = membership.getStartDate() != null ? membership.getStartDate()
                : membership.getJoinDate() != null ? membership.getJoinDate() : LocalDate.now();
        premiumPlanHistoryRepository.save(MembershipPremiumPlanHistoryEntity.builder()
                .membershipId(membership.getId())
                .planId(membership.getPlanId())
                .effectiveFrom(from)
                .createdAt(LocalDateTime.now())
                .createdBy(actor(actor))
                .build());
    }


    private MembershipChangeResponse submitOrApplyDependentChange(
            MembershipChangeRequestEntity change,
            MembershipEntity membership,
            String actor
    ) {
        if (requiresDependentApproval(membership)) {
            ApprovalRequestResponse approval = submit(
                    change, membership, ApprovalType.MEMBERSHIP_DEPENDENT_CHANGE, actor);
            change.setApprovalRequestId(approval.getId());
            change.setUpdatedAt(LocalDateTime.now());
            change.setUpdatedBy(actor);
            return toResponse(changeRequestRepository.save(change));
        }
        applyDependentChange(change, actor);
        return toResponse(changeRequestRepository.findById(change.getId()).orElseThrow());
    }

    private boolean requiresDependentApproval(MembershipEntity membership) {
        LocalDate createdDate = membership.getCreatedAt() == null
                ? membership.getStartDate() == null
                    ? membership.getJoinDate() == null ? LocalDate.now() : membership.getJoinDate()
                    : membership.getStartDate()
                : membership.getCreatedAt().toLocalDate();
        return !LocalDate.now().isBefore(createdDate.plusMonths(1));
    }

    private void validateDependentPartner(
            MembershipEntity membership,
            String partnerId,
            String currentPartnerId
    ) {
        if (partnerId.equals(membership.getMemberId())) {
            throw new IllegalArgumentException("The membership holder cannot also be added as a dependent");
        }
        var partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Dependent partner was not found: " + partnerId));
        if (Status.DECEASED.equalsIgnoreCase(partner.getStatus())
                && !partnerId.equals(currentPartnerId)
                && !(currentPartnerId == null && allowDeceasedDependentAdd())) {
            throw new IllegalArgumentException(
                    "A deceased partner cannot be added as a new dependent unless the membership policy override is enabled");
        }
    }

    private boolean allowDeceasedDependentAdd() {
        String configured = settingService.getSetting("ALLOW_DECEASED_DEPENDENT_ADD", "MEMBERSHIP");
        if (configured == null) return false;
        String value = configured.trim();
        return "1".equals(value)
                || "true".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value);
    }

    private MembershipDependentEntity getVisibleDependent(String membershipId, String dependentId) {
        MembershipDependentEntity dependent = membershipDependentRepository
                .findByIdAndMembershipId(dependentId, membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Dependent was not found on this membership"));
        if (!Set.of(MembershipDependentStatus.ACTIVE, MembershipDependentStatus.DECEASED)
                .contains(dependent.getStatus())) {
            throw new IllegalArgumentException("The dependent is no longer active on this membership");
        }
        return dependent;
    }

    private void applyDependentChange(MembershipChangeRequestEntity change, String actor) {
        if (change.getStatus() == MembershipChangeStatus.APPLIED) return;
        LocalDate effectiveDate = change.getEffectiveDate() == null ? LocalDate.now() : change.getEffectiveDate();
        MembershipDependentEntity affected;

        switch (change.getChangeType()) {
            case ADD_DEPENDENT -> affected = activateDependent(
                    change.getMembershipId(),
                    change.getNewDependentPartnerId(),
                    DependentType.valueOf(change.getNewDependentType()),
                    change,
                    effectiveDate,
                    actor
            );
            case REMOVE_DEPENDENT -> {
                affected = getVisibleDependent(change.getMembershipId(), change.getOldDependentId());
                affected.setActive(false);
                affected.setStatus(MembershipDependentStatus.REMOVED);
                affected.setEffectiveTo(effectiveDate);
                affected.setStatusReason(change.getReason());
                affected.setSourceChangeRequestId(change.getId());
                affected.setUpdatedBy(actor);
                membershipDependentRepository.save(affected);
            }
            case REPLACE_DEPENDENT -> {
                MembershipDependentEntity previous = getVisibleDependent(
                        change.getMembershipId(), change.getOldDependentId());
                MembershipDependentEntity replacement = activateDependent(
                        change.getMembershipId(),
                        change.getNewDependentPartnerId(),
                        DependentType.valueOf(change.getNewDependentType()),
                        change,
                        effectiveDate,
                        actor
                );
                previous.setActive(false);
                previous.setStatus(MembershipDependentStatus.REPLACED);
                previous.setEffectiveTo(effectiveDate);
                previous.setStatusReason(change.getReason());
                previous.setSourceChangeRequestId(change.getId());
                previous.setReplacedByDependentId(replacement.getId());
                previous.setUpdatedBy(actor);
                membershipDependentRepository.save(previous);
                affected = replacement;
            }
            default -> throw new IllegalStateException("Unsupported dependent change type: " + change.getChangeType());
        }

        change.setStatus(MembershipChangeStatus.APPLIED);
        change.setAppliedAt(LocalDateTime.now());
        change.setAppliedBy(actor);
        change.setUpdatedAt(LocalDateTime.now());
        change.setUpdatedBy(actor);
        changeRequestRepository.save(change);
        membershipUpdateHandlerRegistry.handleUpdate(change.getMembershipId());
        audit(change, "APPLIED", dependentOldValue(change), dependentNewValue(change),
                "Membership dependent change applied", actor);
    }

    private MembershipDependentEntity activateDependent(
            String membershipId,
            String partnerId,
            DependentType dependentType,
            MembershipChangeRequestEntity change,
            LocalDate effectiveDate,
            String actor
    ) {
        MembershipDependentEntity dependent = membershipDependentRepository
                .findFirstByMembershipIdAndDependentPartnerIdOrderByCreatedAtDesc(membershipId, partnerId)
                .orElseGet(MembershipDependentEntity::new);
        if (dependent.getId() != null
                && Set.of(MembershipDependentStatus.ACTIVE, MembershipDependentStatus.DECEASED)
                .contains(dependent.getStatus())) {
            if (change.getChangeType() == MembershipChangeType.REPLACE_DEPENDENT
                    && dependent.getId().equals(change.getOldDependentId())) {
                dependent.setDependentType(dependentType);
                dependent.setUpdatedBy(actor);
                dependent.setSourceChangeRequestId(change.getId());
                return membershipDependentRepository.save(dependent);
            }
            throw new IllegalArgumentException("The dependent is already active on this membership");
        }
        dependent.setMembershipId(membershipId);
        dependent.setDependentPartnerId(partnerId);
        dependent.setDependentType(dependentType);
        dependent.setActive(true);
        dependent.setStatus(MembershipDependentStatus.ACTIVE);
        dependent.setEffectiveFrom(effectiveDate);
        dependent.setEffectiveTo(null);
        dependent.setDeceasedDate(null);
        dependent.setStatusReason(null);
        dependent.setSourceChangeRequestId(change.getId());
        dependent.setReplacedByDependentId(null);
        if (dependent.getCreatedBy() == null) dependent.setCreatedBy(actor);
        dependent.setUpdatedBy(actor);
        return membershipDependentRepository.save(dependent);
    }

    private boolean isDependentChange(MembershipChangeType type) {
        return type == MembershipChangeType.ADD_DEPENDENT
                || type == MembershipChangeType.REMOVE_DEPENDENT
                || type == MembershipChangeType.REPLACE_DEPENDENT;
    }

    private Object dependentOldValue(MembershipChangeRequestEntity change) {
        if (clean(change.getOldDependentPartnerId()) == null) return null;
        return Map.of(
                "dependentId", change.getOldDependentId() == null ? "" : change.getOldDependentId(),
                "dependentPartnerId", change.getOldDependentPartnerId(),
                "dependentType", change.getOldDependentType() == null ? "" : change.getOldDependentType()
        );
    }

    private Object dependentNewValue(MembershipChangeRequestEntity change) {
        if (clean(change.getNewDependentPartnerId()) == null) return null;
        return Map.of(
                "dependentPartnerId", change.getNewDependentPartnerId(),
                "dependentType", change.getNewDependentType() == null ? "" : change.getNewDependentType()
        );
    }

    private void applyTransfer(MembershipChangeRequestEntity change, String actor) {
        MembershipEntity membership = getMembership(change.getMembershipId());
        String previous = membership.getMemberId();
        membership.setMemberId(change.getNewMemberId());
        membership.setUpdatedAt(LocalDateTime.now());
        membership.setUpdatedBy(actor);
        membershipRepository.save(membership);
        partnerService.addRole(change.getNewMemberId(), "MEMBER");
        if (previous != null && !previous.equals(change.getNewMemberId())
                && !membershipRepository.existsByMemberId(previous)
                && partnerService.getRoles(previous).contains("MEMBER")) {
            partnerService.removeRole(previous, "MEMBER");
        }
        change.setStatus(MembershipChangeStatus.APPLIED);
        change.setAppliedAt(LocalDateTime.now());
        change.setAppliedBy(actor);
        changeRequestRepository.save(change);
        audit(change, "APPLIED", Map.of("memberId", previous), Map.of("memberId", change.getNewMemberId()), "Membership transferred after approval", actor);
    }

    private void applyPlanChange(MembershipChangeRequestEntity change, String actor) {
        if (change.getStatus() == MembershipChangeStatus.APPLIED) return;
        MembershipEntity membership = getMembership(change.getMembershipId());
        ensureBaselineHistory(membership, actor);
        LocalDate effectiveDate = change.getEffectiveDate() == null ? LocalDate.now() : change.getEffectiveDate();
        MembershipPlanHistoryEntity current = planHistoryRepository
                .findFirstByMembershipIdAndEffectiveToIsNullOrderByEffectiveFromDesc(membership.getId()).orElse(null);
        if (current != null && current.getPlanId().equals(change.getNewPlanId())) {
            // Idempotent replay after a partial deployment.
        } else if (current != null && effectiveDate.equals(current.getEffectiveFrom())) {
            // A second zero-wait change approved on the same date replaces the
            // zero-day plan period instead of creating an invalid overlapping row.
            current.setPlanId(change.getNewPlanId());
            current.setSourceChangeRequestId(change.getId());
            planHistoryRepository.save(current);
        } else {
            if (current != null) {
                current.setEffectiveTo(effectiveDate.minusDays(1));
                planHistoryRepository.save(current);
            }
            planHistoryRepository.save(MembershipPlanHistoryEntity.builder()
                    .membershipId(membership.getId()).planId(change.getNewPlanId())
                    .effectiveFrom(effectiveDate).sourceChangeRequestId(change.getId())
                    .createdAt(LocalDateTime.now()).createdBy(actor).build());
        }

        // Deployments upgrading from the earlier all-at-once implementation may
        // still have scheduled requests whose premium was not changed on approval.
        // Apply that premium defensively before marking the benefit change complete.
        if (!change.getNewPlanId().equals(membership.getPlanId())) {
            MembershipPlanEntity plan = membershipPlanRepository.findById(change.getNewPlanId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Approved membership plan no longer exists: " + change.getNewPlanId()));
            ensureBaselinePremiumHistory(membership, actor);
            applyPremiumPlanChange(change, membership, plan, LocalDate.now(), actor);
        }

        change.setStatus(MembershipChangeStatus.APPLIED);
        change.setAppliedAt(LocalDateTime.now());
        change.setAppliedBy(actor);
        change.setUpdatedAt(LocalDateTime.now());
        change.setUpdatedBy(actor);
        changeRequestRepository.save(change);
        audit(change, "APPLIED",
                Map.of("coveragePlanId", change.getOldPlanId()),
                Map.of("coveragePlanId", change.getNewPlanId(), "benefitEffectiveDate", effectiveDate.toString()),
                "Approved membership plan benefits became effective",
                actor);
    }

    private void applyPremiumPlanChange(
            MembershipChangeRequestEntity change,
            MembershipEntity membership,
            MembershipPlanEntity newPlan,
            LocalDate effectiveDate,
            String actor
    ) {
        MembershipPremiumPlanHistoryEntity current = premiumPlanHistoryRepository
                .findFirstByMembershipIdAndEffectiveToIsNullOrderByEffectiveFromDesc(membership.getId())
                .orElse(null);
        if (current != null && current.getPlanId().equals(newPlan.getId())) {
            // Idempotent replay after a partially completed approval.
        } else if (current != null && effectiveDate.equals(current.getEffectiveFrom())) {
            current.setPlanId(newPlan.getId());
            current.setSourceChangeRequestId(change.getId());
            premiumPlanHistoryRepository.save(current);
        } else {
            if (current != null) {
                current.setEffectiveTo(effectiveDate.minusDays(1));
                premiumPlanHistoryRepository.save(current);
            }
            premiumPlanHistoryRepository.save(MembershipPremiumPlanHistoryEntity.builder()
                    .membershipId(membership.getId())
                    .planId(newPlan.getId())
                    .effectiveFrom(effectiveDate)
                    .sourceChangeRequestId(change.getId())
                    .createdAt(LocalDateTime.now())
                    .createdBy(actor)
                    .build());
        }

        membership.setPlanId(newPlan.getId());
        membership.setPremiumCents(calculateTotalPremiumCents(membership.getId(), newPlan, effectiveDate));
        membership.setUpdatedAt(LocalDateTime.now());
        membership.setUpdatedBy(actor);
        membershipRepository.save(membership);
    }

    private ApprovalRequestResponse submit(MembershipChangeRequestEntity change, MembershipEntity membership, ApprovalType approvalType, String actor) {
        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(approvalType);
        request.setReferenceId(change.getId());
        request.setReferenceNo(membership.getMembershipNo());
        String membershipHolder = firstNonBlank(partnerName(membership.getMemberId()), membership.getMembershipNo());
        String changeLabel = change.getChangeType() == null
                ? "change"
                : change.getChangeType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        request.setTitle(switch (approvalType) {
            case MEMBERSHIP_TRANSFER -> "Membership transfer - " + membership.getMembershipNo()
                    + " - " + partnerName(change.getOldMemberId()) + " to " + partnerName(change.getNewMemberId());
            case MEMBERSHIP_PLAN_CHANGE -> "Membership plan change - " + membership.getMembershipNo()
                    + " - " + membershipHolder + " - " + planName(change.getOldPlanId())
                    + " to " + planName(change.getNewPlanId());
            case MEMBERSHIP_DEPENDENT_CHANGE -> "Membership dependent "
                    + changeLabel
                    + " - " + membership.getMembershipNo() + " - "
                    + firstNonBlank(
                    partnerName(change.getNewDependentPartnerId()),
                    partnerName(change.getOldDependentPartnerId()),
                    membershipHolder);
            case MEMBERSHIP_MERGE -> "Membership merge - " + membership.getMembershipNo();
            default -> "Membership change - " + membership.getMembershipNo() + " - " + membershipHolder;
        });
        request.setDescription(approvalDescription(change, membership, approvalType));
        request.setRequesterId(actor);
        request.setPayloadJson(toJson(buildApprovalPayload(change, membership, approvalType)));
        return approvalServiceProvider.getObject().submitForApproval(request);
    }

    private String approvalDescription(
            MembershipChangeRequestEntity change,
            MembershipEntity membership,
            ApprovalType approvalType
    ) {
        if (approvalType == ApprovalType.MEMBERSHIP_PLAN_CHANGE) {
            return "Review the current and proposed membership plans and premium impact. " +
                    "On approval the new premium is immediate; downgrades also apply benefits immediately, " +
                    "while upgrades apply the configured benefit waiting period. Reason: " + change.getReason();
        }
        if (approvalType == ApprovalType.MEMBERSHIP_TRANSFER) {
            return "Review the current and proposed membership holders before actioning. Reason: " + change.getReason();
        }
        if (approvalType == ApprovalType.MEMBERSHIP_DEPENDENT_CHANGE) {
            return "Review the dependent change, relationship, effective date, and request reason before actioning. " +
                    "Reason: " + change.getReason();
        }
        if (approvalType == ApprovalType.MEMBERSHIP_MERGE) {
            return "Review the primary and source memberships, dependants, premium periods, payments and claims before actioning. " +
                    "The source membership will become a read-only merged alias. Reason: " + change.getReason();
        }
        return change.getReason();
    }

    private Map<String, Object> buildApprovalPayload(
            MembershipChangeRequestEntity change,
            MembershipEntity membership,
            ApprovalType approvalType
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("membershipId", membership.getId());
        summary.put("membershipNumber", membership.getMembershipNo());
        summary.put("membershipHolder", partnerName(membership.getMemberId()));
        summary.put("membershipStatus", membership.getStatus());
        summary.put("changeType", change.getChangeType());
        summary.put("reason", change.getReason());
        summary.put("requestedAt", change.getRequestedAt());
        summary.put("requestedBy", change.getRequestedBy());
        payload.put("requestSummary", summary);

        Map<String, Object> current = new LinkedHashMap<>();
        Map<String, Object> proposed = new LinkedHashMap<>();
        Map<String, Object> impact = new LinkedHashMap<>();

        if (approvalType == ApprovalType.MEMBERSHIP_PLAN_CHANGE) {
            MembershipPlanEntity currentPlan = membershipPlanRepository.findById(change.getOldPlanId()).orElse(null);
            MembershipPlanEntity newPlan = membershipPlanRepository.findById(change.getNewPlanId()).orElse(null);
            int waitingMonths = change.getWaitingPeriodMonths() == null
                    ? DEFAULT_WAITING_PERIOD_MONTHS : change.getWaitingPeriodMonths();
            LocalDate premiumEffectiveDate = LocalDate.now();
            Long estimatedNewPremium = estimatedPremiumCents(membership.getId(), newPlan, premiumEffectiveDate);
            boolean downgrade = isPlanDowngrade(change.getOldPlanId(), newPlan);
            LocalDate benefitEffectiveDate = downgrade
                    ? premiumEffectiveDate
                    : premiumEffectiveDate.plusMonths(waitingMonths);

            current.put("planId", change.getOldPlanId());
            current.put("planCode", currentPlan == null ? null : currentPlan.getPlanCode());
            current.put("planName", planName(change.getOldPlanId()));
            current.put("monthlyPremiumCents", membership.getPremiumCents());
            current.put("maximumDependents", currentPlan == null ? null : currentPlan.getMaxDependents());

            proposed.put("planId", change.getNewPlanId());
            proposed.put("planCode", newPlan == null ? null : newPlan.getPlanCode());
            proposed.put("planName", planName(change.getNewPlanId()));
            proposed.put("basePremiumCents", newPlan == null ? null : newPlan.getPremiumCents());
            proposed.put("estimatedMonthlyPremiumCents", estimatedNewPremium);
            proposed.put("maximumDependents", newPlan == null ? null : newPlan.getMaxDependents());

            impact.put("changeDirection", downgrade ? "DOWNGRADE" : "UPGRADE");
            impact.put("premiumEffectiveDateIfApprovedToday", premiumEffectiveDate);
            impact.put("benefitWaitingPeriodMonths", downgrade ? 0 : waitingMonths);
            impact.put("benefitEffectiveDateIfApprovedToday", benefitEffectiveDate);
            impact.put("coverBeforeBenefitEffectiveDate", planName(change.getOldPlanId()));
            impact.put("coverFromBenefitEffectiveDate", planName(change.getNewPlanId()));
        } else if (approvalType == ApprovalType.MEMBERSHIP_TRANSFER) {
            current.put("memberId", change.getOldMemberId());
            current.put("memberName", partnerName(change.getOldMemberId()));
            proposed.put("memberId", change.getNewMemberId());
            proposed.put("memberName", partnerName(change.getNewMemberId()));
            impact.put("effectiveDate", "On final approval");
        } else if (approvalType == ApprovalType.MEMBERSHIP_DEPENDENT_CHANGE) {
            current.put("dependentId", change.getOldDependentId());
            current.put("dependentPartnerId", change.getOldDependentPartnerId());
            current.put("dependentName", partnerName(change.getOldDependentPartnerId()));
            current.put("dependentType", change.getOldDependentType());
            proposed.put("dependentPartnerId", change.getNewDependentPartnerId());
            proposed.put("dependentName", partnerName(change.getNewDependentPartnerId()));
            proposed.put("dependentType", change.getNewDependentType());
            impact.put("effectiveDate", change.getEffectiveDate() == null ? "On final approval" : change.getEffectiveDate());
        } else if (approvalType == ApprovalType.MEMBERSHIP_MERGE) {
            MembershipEntity source = membershipRepository.findById(change.getSourceMembershipId()).orElse(null);
            current.put("sourceMembershipId", change.getSourceMembershipId());
            current.put("sourceMembershipNumber", source == null ? null : source.getMembershipNo());
            current.put("sourceMember", source == null ? null : partnerName(source.getMemberId()));
            current.put("sourceStatus", source == null ? null : source.getStatus());
            current.put("sourcePlan", source == null ? null : planName(source.getPlanId()));
            proposed.put("primaryMembershipId", membership.getId());
            proposed.put("primaryMembershipNumber", membership.getMembershipNo());
            proposed.put("primaryMember", partnerName(membership.getMemberId()));
            proposed.put("primaryStatus", membership.getStatus());
            proposed.put("primaryPlan", planName(membership.getPlanId()));
            impact.put("sourceStatusAfterApproval", "MERGED");
            impact.put("effectiveDate", "On final approval");
        } else {
            payload.put("change", toResponse(change));
        }

        if (!current.isEmpty()) payload.put("currentValues", current);
        if (!proposed.isEmpty()) payload.put("proposedValues", proposed);
        if (!impact.isEmpty()) payload.put("approvalImpact", impact);
        payload.put("attachmentObjectIds", List.of(membership.getId(), change.getId()));
        return payload;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "Not specified";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "Not specified";
    }

    private boolean isPlanDowngrade(String oldPlanId, MembershipPlanEntity newPlan) {
        if (newPlan == null) return false;
        MembershipPlanEntity currentPlan = membershipPlanRepository.findById(oldPlanId).orElse(null);
        if (currentPlan == null) return false;
        long oldBasePremium = currentPlan.getPremiumCents() == null ? 0L : currentPlan.getPremiumCents();
        long newBasePremium = newPlan.getPremiumCents() == null ? 0L : newPlan.getPremiumCents();
        return newBasePremium < oldBasePremium;
    }

    private Long estimatedPremiumCents(
            String membershipId,
            MembershipPlanEntity plan,
            LocalDate effectiveDate
    ) {
        if (plan == null) return null;
        try {
            return calculateTotalPremiumCents(membershipId, plan, effectiveDate);
        } catch (Exception ignored) {
            return plan.getPremiumCents();
        }
    }

    private void requireNoOpenChange(String membershipId) {
        List<MembershipChangeStatus> open = List.of(
                MembershipChangeStatus.PENDING_APPROVAL, MembershipChangeStatus.APPROVED_SCHEDULED);
        if (changeRequestRepository.existsByMembershipIdAndStatusIn(membershipId, open)
                || changeRequestRepository.existsBySourceMembershipIdAndStatusIn(membershipId, open)) {
            throw new IllegalStateException("This membership already has a pending or scheduled change");
        }
    }

    private void requireNoPendingMergeBlockers(String membershipId) {
        Integer pendingStatusActions = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM membership_status_change_request
             WHERE membership_id = ? AND status = 'PENDING_APPROVAL'
            """, Integer.class, membershipId);
        Integer pendingClaims = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
              FROM membership_claim c
              JOIN approval_request a ON a.reference_id = c.id
             WHERE c.membership_id = ? AND a.status = 'PENDING'
            """, Integer.class, membershipId);
        if ((pendingStatusActions != null && pendingStatusActions > 0)
                || (pendingClaims != null && pendingClaims > 0)) {
            throw new IllegalStateException(
                    "Complete the membership's pending status changes or claims before requesting a merge");
        }
    }

    private void requireNoConflictingDependentChange(
            String membershipId,
            String oldDependentId,
            String newDependentPartnerId
    ) {
        List<MembershipChangeStatus> openStatuses = List.of(
                MembershipChangeStatus.PENDING_APPROVAL,
                MembershipChangeStatus.APPROVED_SCHEDULED
        );
        List<MembershipChangeType> dependentChanges = List.of(
                MembershipChangeType.ADD_DEPENDENT,
                MembershipChangeType.REMOVE_DEPENDENT,
                MembershipChangeType.REPLACE_DEPENDENT
        );

        if (newDependentPartnerId != null
                && changeRequestRepository.existsByMembershipIdAndNewDependentPartnerIdAndChangeTypeInAndStatusIn(
                        membershipId, newDependentPartnerId, dependentChanges, openStatuses)) {
            throw new IllegalStateException("The selected person already has a pending dependent change on this membership");
        }

        if (oldDependentId != null
                && changeRequestRepository.existsByMembershipIdAndOldDependentIdAndChangeTypeInAndStatusIn(
                        membershipId, oldDependentId,
                        List.of(MembershipChangeType.REMOVE_DEPENDENT, MembershipChangeType.REPLACE_DEPENDENT),
                        openStatuses)) {
            throw new IllegalStateException("The selected dependent already has a pending removal or replacement");
        }
    }

    private MembershipEntity getMembership(String id) {
        return membershipRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Membership not found: " + id));
    }

    private MembershipEntity getMembershipForUpdate(String id) {
        return membershipRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + id));
    }

    private Long calculateTotalPremiumCents(String membershipId, MembershipPlanEntity plan, LocalDate effectiveDate) {
        long total = plan.getPremiumCents() == null ? 0L : plan.getPremiumCents();
        LocalDate ageDate = effectiveDate == null ? LocalDate.now() : effectiveDate;
        for (MembershipDependentEntity dependent : membershipDependentRepository.findByMembershipIdAndStatus(
                membershipId, MembershipDependentStatus.ACTIVE)) {
            var partner = partnerRepository.findById(dependent.getDependentPartnerId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Dependent partner not found while applying plan change: " + dependent.getDependentPartnerId()));
            if (partner.getBirthDate() == null) {
                throw new IllegalStateException(
                        "Dependent birth date is required before the approved plan change can take effect: " + partner.getId());
            }
            LocalDate birthDate = toLocalDate(partner.getBirthDate());
            int age = Period.between(birthDate, ageDate).getYears();
            total += membershipPlanPremiumRuleService.resolveAdditionalPremiumCents(
                    plan.getId(), dependent.getDependentType(), age);
        }
        return total;
    }

    private LocalDate toLocalDate(java.util.Date value) {
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private MembershipChangeRequestEntity getChange(String id) {
        return changeRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Membership change request not found: " + id));
    }

    private void audit(MembershipChangeRequestEntity change, String event, Object oldValue, Object newValue, String details, String actor) {
        auditRepository.save(MembershipChangeAuditEntity.builder()
                .membershipId(change.getMembershipId()).changeRequestId(change.getId()).eventType(event)
                .oldValuesJson(oldValue == null ? null : toJson(oldValue))
                .newValuesJson(newValue == null ? null : toJson(newValue))
                .details(details).performedBy(actor(actor)).performedAt(LocalDateTime.now()).build());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialise membership change details", exception);
        }
    }

    private MembershipChangeConfigurationDto toConfigurationDto(MembershipChangeConfigurationEntity entity) {
        return MembershipChangeConfigurationDto.builder()
                .planChangeWaitingPeriodMonths(entity.getPlanChangeWaitingPeriodMonths())
                .updatedBy(entity.getUpdatedBy()).build();
    }

    private MembershipChangeResponse toResponse(MembershipChangeRequestEntity e) {
        return MembershipChangeResponse.builder().id(e.getId()).membershipId(e.getMembershipId())
                .sourceMembershipId(e.getSourceMembershipId())
                .changeType(e.getChangeType()).status(e.getStatus())
                .oldMemberId(e.getOldMemberId()).oldMemberName(partnerName(e.getOldMemberId()))
                .newMemberId(e.getNewMemberId()).newMemberName(partnerName(e.getNewMemberId()))
                .oldPlanId(e.getOldPlanId()).oldPlanName(planName(e.getOldPlanId()))
                .newPlanId(e.getNewPlanId()).newPlanName(planName(e.getNewPlanId()))
                .oldDependentId(e.getOldDependentId())
                .oldDependentPartnerId(e.getOldDependentPartnerId())
                .oldDependentName(partnerName(e.getOldDependentPartnerId()))
                .newDependentPartnerId(e.getNewDependentPartnerId())
                .newDependentName(partnerName(e.getNewDependentPartnerId()))
                .oldDependentType(e.getOldDependentType())
                .newDependentType(e.getNewDependentType())
                .waitingPeriodMonths(e.getWaitingPeriodMonths())
                .effectiveDate(e.getEffectiveDate()).reason(e.getReason()).approvalRequestId(e.getApprovalRequestId())
                .requestedAt(e.getRequestedAt()).requestedBy(e.getRequestedBy()).approvedAt(e.getApprovedAt()).approvedBy(e.getApprovedBy())
                .appliedAt(e.getAppliedAt()).appliedBy(e.getAppliedBy()).build();
    }

    private MembershipChangeAuditResponse toAuditResponse(MembershipChangeAuditEntity e) {
        return MembershipChangeAuditResponse.builder().id(e.getId()).membershipId(e.getMembershipId()).changeRequestId(e.getChangeRequestId())
                .eventType(e.getEventType()).oldValuesJson(e.getOldValuesJson()).newValuesJson(e.getNewValuesJson())
                .details(e.getDetails()).performedBy(e.getPerformedBy()).performedAt(e.getPerformedAt()).build();
    }


    private String partnerName(String partnerId) {
        if (clean(partnerId) == null) return null;
        return partnerRepository.findById(partnerId).map(partner -> {
            String name = String.join(" ",
                    clean(partner.getName2()) == null ? "" : clean(partner.getName2()),
                    clean(partner.getName3()) == null ? "" : clean(partner.getName3()),
                    clean(partner.getName1()) == null ? "" : clean(partner.getName1())).trim();
            return name.isEmpty() ? partnerId : name.replaceAll("\\s+", " ");
        }).orElse(partnerId);
    }

    private String planName(String planId) {
        if (clean(planId) == null) return null;
        return membershipPlanRepository.findById(planId)
                .map(plan -> clean(plan.getName()) == null ? planId : plan.getName().trim())
                .orElse(planId);
    }

    private String requireReason(String value) {
        String reason = clean(value);
        if (reason == null) throw new IllegalArgumentException("A reason is required");
        return reason;
    }

    private String actor(String value) { String cleaned = clean(value); return cleaned == null ? "SYSTEM" : cleaned; }
    private String clean(String value) { if (value == null) return null; String v = value.trim(); return v.isEmpty() ? null : v; }
}
