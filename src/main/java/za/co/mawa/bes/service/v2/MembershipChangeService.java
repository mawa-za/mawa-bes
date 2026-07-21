package za.co.mawa.bes.service.v2;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.ApprovalRequestResponse;
import za.co.mawa.bes.dto.v2.ApprovalSubmitRequest;
import za.co.mawa.bes.dto.v2.membership.change.*;
import za.co.mawa.bes.entity.v2.*;
import za.co.mawa.bes.enums.*;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.v2.*;
import za.co.mawa.bes.service.PartnerService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
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
    private final MembershipChangeAuditRepository auditRepository;
    private final PartnerRepository partnerRepository;
    private final MembershipDependentRepository membershipDependentRepository;
    private final MembershipPlanPremiumRuleService membershipPlanPremiumRuleService;
    private final PartnerService partnerService;
    private final ObjectProvider<ApprovalService> approvalServiceProvider;
    private final Gson gson;

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
        requireNoOpenChange(membershipId);
        String newMemberId = clean(request == null ? null : request.getNewMemberId());
        if (newMemberId == null) throw new IllegalArgumentException("New member is required");
        if (newMemberId.equals(membership.getMemberId())) throw new IllegalArgumentException("The selected partner is already the membership holder");
        if (!partnerRepository.existsById(newMemberId)) throw new IllegalArgumentException("New member partner was not found: " + newMemberId);

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
        String actor = actor(actionBy);
        change.setApprovedAt(LocalDateTime.now());
        change.setApprovedBy(actor);
        change.setUpdatedAt(LocalDateTime.now());
        change.setUpdatedBy(actor);
        if (change.getChangeType() == MembershipChangeType.TRANSFER) {
            change.setEffectiveDate(LocalDate.now());
            changeRequestRepository.save(change);
            audit(change, "APPROVED",
                    Map.of("memberId", change.getOldMemberId()),
                    Map.of("memberId", change.getNewMemberId(), "effectiveDate", change.getEffectiveDate().toString()),
                    "Membership transfer approved",
                    actor);
            applyTransfer(change, actor);
        } else {
            int waitingMonths = change.getWaitingPeriodMonths() == null ? DEFAULT_WAITING_PERIOD_MONTHS : change.getWaitingPeriodMonths();
            change.setEffectiveDate(LocalDate.now().plusMonths(waitingMonths));
            change.setStatus(MembershipChangeStatus.APPROVED_SCHEDULED);
            changeRequestRepository.save(change);
            audit(change, "APPROVED", Map.of("planId", change.getOldPlanId()), Map.of("planId", change.getNewPlanId(), "effectiveDate", change.getEffectiveDate().toString()), "Plan change approved and scheduled", actor);
            if (!change.getEffectiveDate().isAfter(LocalDate.now())) applyPlanChange(change, actor);
        }
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
        MembershipPlanEntity plan = membershipPlanRepository.findById(change.getNewPlanId())
                .orElseThrow(() -> new IllegalStateException("Approved membership plan no longer exists: " + change.getNewPlanId()));
        String previousPlan = membership.getPlanId();
        membership.setPlanId(plan.getId());
        membership.setPremiumCents(calculateTotalPremiumCents(membership.getId(), plan, effectiveDate));
        membership.setUpdatedAt(LocalDateTime.now());
        membership.setUpdatedBy(actor);
        membershipRepository.save(membership);
        change.setStatus(MembershipChangeStatus.APPLIED);
        change.setAppliedAt(LocalDateTime.now());
        change.setAppliedBy(actor);
        change.setUpdatedAt(LocalDateTime.now());
        change.setUpdatedBy(actor);
        changeRequestRepository.save(change);
        audit(change, "APPLIED", Map.of("planId", previousPlan), Map.of("planId", plan.getId(), "effectiveDate", effectiveDate.toString()), "Approved plan change became effective", actor);
    }

    private ApprovalRequestResponse submit(MembershipChangeRequestEntity change, MembershipEntity membership, ApprovalType approvalType, String actor) {
        ApprovalSubmitRequest request = new ApprovalSubmitRequest();
        request.setApprovalType(approvalType);
        request.setReferenceId(change.getId());
        request.setReferenceNo(membership.getMembershipNo());
        request.setTitle(approvalType == ApprovalType.MEMBERSHIP_TRANSFER
                ? "Membership Transfer: " + membership.getMembershipNo()
                : "Membership Plan Change: " + membership.getMembershipNo());
        request.setDescription(change.getReason());
        request.setRequesterId(actor);
        request.setPayloadJson(gson.toJson(toResponse(change)));
        return approvalServiceProvider.getObject().submitForApproval(request);
    }

    private void requireNoOpenChange(String membershipId) {
        if (changeRequestRepository.existsByMembershipIdAndStatusIn(membershipId,
                List.of(MembershipChangeStatus.PENDING_APPROVAL, MembershipChangeStatus.APPROVED_SCHEDULED))) {
            throw new IllegalStateException("This membership already has a pending or scheduled change");
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
        for (MembershipDependentEntity dependent : membershipDependentRepository.findByMembershipId(membershipId)) {
            var partner = partnerRepository.findById(dependent.getDependentPartnerId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Dependent partner not found while applying plan change: " + dependent.getDependentPartnerId()));
            if (partner.getBirthDate() == null) {
                throw new IllegalStateException(
                        "Dependent birth date is required before the approved plan change can take effect: " + partner.getId());
            }
            LocalDate birthDate = partner.getBirthDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            int age = Period.between(birthDate, ageDate).getYears();
            total += membershipPlanPremiumRuleService.resolveAdditionalPremiumCents(
                    plan.getId(), dependent.getDependentType(), age);
        }
        return total;
    }

    private MembershipChangeRequestEntity getChange(String id) {
        return changeRequestRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Membership change request not found: " + id));
    }

    private void audit(MembershipChangeRequestEntity change, String event, Object oldValue, Object newValue, String details, String actor) {
        auditRepository.save(MembershipChangeAuditEntity.builder()
                .membershipId(change.getMembershipId()).changeRequestId(change.getId()).eventType(event)
                .oldValuesJson(oldValue == null ? null : gson.toJson(oldValue))
                .newValuesJson(newValue == null ? null : gson.toJson(newValue))
                .details(details).performedBy(actor(actor)).performedAt(LocalDateTime.now()).build());
    }

    private MembershipChangeConfigurationDto toConfigurationDto(MembershipChangeConfigurationEntity entity) {
        return MembershipChangeConfigurationDto.builder()
                .planChangeWaitingPeriodMonths(entity.getPlanChangeWaitingPeriodMonths())
                .updatedBy(entity.getUpdatedBy()).build();
    }

    private MembershipChangeResponse toResponse(MembershipChangeRequestEntity e) {
        return MembershipChangeResponse.builder().id(e.getId()).membershipId(e.getMembershipId())
                .changeType(e.getChangeType()).status(e.getStatus())
                .oldMemberId(e.getOldMemberId()).oldMemberName(partnerName(e.getOldMemberId()))
                .newMemberId(e.getNewMemberId()).newMemberName(partnerName(e.getNewMemberId()))
                .oldPlanId(e.getOldPlanId()).oldPlanName(planName(e.getOldPlanId()))
                .newPlanId(e.getNewPlanId()).newPlanName(planName(e.getNewPlanId()))
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
