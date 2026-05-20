package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanClaimPayoutResponseDto;
import za.co.mawa.bes.entity.v2.MembershipPlanClaimPayoutEntity;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.enums.MembershipClaimType;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.repository.v2.MembershipPlanClaimPayoutRepository;
import za.co.mawa.bes.repository.v2.MembershipPlanRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPlanClaimPayoutService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipPlanClaimPayoutRepository payoutRepository;

    @Transactional
    public MembershipPlanClaimPayoutResponseDto create(
            String planId,
            MembershipPlanClaimPayoutRequestDto dto,
            String username
    ) {
        MembershipPlanEntity plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Membership plan not found"));

        validateRequest(dto);

        DependentType dependentType = resolveDependentType(dto.getDependentType());

        boolean exists = payoutRepository.existsByPlanIdAndClaimTypeAndDependentType(
                planId,
                dto.getClaimType(),
                dependentType
        );

        if (exists) {
            throw new RuntimeException("Payout rule already exists for this plan, claim type and dependent type");
        }

        MembershipPlanClaimPayoutEntity entity = new MembershipPlanClaimPayoutEntity();
        entity.setPlan(plan);
        entity.setClaimType(dto.getClaimType());
        entity.setDependentType(dependentType);
        entity.setPayoutAmountCents(dto.getPayoutAmountCents());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(username);

        return toDto(payoutRepository.save(entity));
    }

    @Transactional
    public MembershipPlanClaimPayoutResponseDto update(
            String planId,
            String payoutId,
            MembershipPlanClaimPayoutRequestDto dto,
            String username
    ) {
        MembershipPlanClaimPayoutEntity entity = payoutRepository.findByPlanIdAndId(planId, payoutId)
                .orElseThrow(() -> new RuntimeException("Payout rule not found for this membership plan"));

        validateRequest(dto);

        DependentType dependentType = resolveDependentType(dto.getDependentType());

        boolean duplicateExists = payoutRepository.existsByPlanIdAndClaimTypeAndDependentTypeAndIdNot(
                planId,
                dto.getClaimType(),
                dependentType,
                payoutId
        );

        if (duplicateExists) {
            throw new RuntimeException("Another payout rule already exists for this plan, claim type and dependent type");
        }

        entity.setClaimType(dto.getClaimType());
        entity.setDependentType(dependentType);
        entity.setPayoutAmountCents(dto.getPayoutAmountCents());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.getActive());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(username);

        return toDto(payoutRepository.save(entity));
    }

    public List<MembershipPlanClaimPayoutResponseDto> getActiveByPlan(String planId) {
        return payoutRepository.findByPlanIdAndActiveTrue(planId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<MembershipPlanClaimPayoutResponseDto> getAllByPlan(String planId) {
        return payoutRepository.findByPlanId(planId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deactivate(String planId, String payoutId, String username) {
        MembershipPlanClaimPayoutEntity entity = payoutRepository.findByPlanIdAndId(planId, payoutId)
                .orElseThrow(() -> new RuntimeException("Payout rule not found for this membership plan"));

        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(username);

        payoutRepository.save(entity);
    }

    /**
     * Used by claim creation.
     * First tries an exact rule, for example FUNERAL + CHILD.
     * If no exact rule exists, it falls back to FUNERAL + ANY.
     */
    public Long resolvePayoutAmountCents(
            String planId,
            MembershipClaimType claimType,
            DependentType dependentType
    ) {
        DependentType exactDependentType = resolveDependentType(dependentType);

        return payoutRepository
                .findByPlanIdAndClaimTypeAndDependentTypeAndActiveTrue(
                        planId,
                        claimType,
                        exactDependentType
                )
                .or(() -> payoutRepository.findByPlanIdAndClaimTypeAndDependentTypeAndActiveTrue(
                        planId,
                        claimType,
                        DependentType.ANY
                ))
                .map(MembershipPlanClaimPayoutEntity::getPayoutAmountCents)
                .orElseThrow(() -> new RuntimeException(
                        "No payout rule configured for this plan, claim type and dependent type"
                ));
    }

    private void validateRequest(MembershipPlanClaimPayoutRequestDto dto) {
        if (dto.getClaimType() == null) {
            throw new RuntimeException("Claim type is required");
        }

        if (dto.getPayoutAmountCents() == null || dto.getPayoutAmountCents() < 0) {
            throw new RuntimeException("Payout amount must be zero or greater");
        }
    }

    private DependentType resolveDependentType(DependentType dependentType) {
        return dependentType != null ? dependentType : DependentType.ANY;
    }

    private MembershipPlanClaimPayoutResponseDto toDto(MembershipPlanClaimPayoutEntity entity) {
        return MembershipPlanClaimPayoutResponseDto.builder()
                .id(entity.getId())
                .planId(entity.getPlan().getId())
                .planCode(entity.getPlan().getPlanCode())
                .planName(entity.getPlan().getName())
                .claimType(entity.getClaimType())
                .dependentType(entity.getDependentType())
                .payoutAmountCents(entity.getPayoutAmountCents())
                .active(entity.getActive())
                .build();
    }
}
