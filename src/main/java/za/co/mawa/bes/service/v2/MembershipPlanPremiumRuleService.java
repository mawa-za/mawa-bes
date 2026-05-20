package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleRequestDto;
import za.co.mawa.bes.dto.v2.MembershipPlanPremiumRuleResponseDto;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.entity.v2.MembershipPlanPremiumRuleEntity;
import za.co.mawa.bes.enums.DependentType;
import za.co.mawa.bes.repository.v2.MembershipPlanPremiumRuleRepository;
import za.co.mawa.bes.repository.v2.MembershipPlanRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPlanPremiumRuleService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipPlanPremiumRuleRepository premiumRuleRepository;

    @Transactional
    public MembershipPlanPremiumRuleResponseDto create(
            String planId,
            MembershipPlanPremiumRuleRequestDto dto,
            String username
    ) {
        MembershipPlanEntity plan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Membership plan not found"));

        validate(dto);

        List<MembershipPlanPremiumRuleEntity> overlappingRules =
                premiumRuleRepository.findOverlappingRules(
                        planId,
                        dto.getDependentType(),
                        dto.getMinAge(),
                        dto.getMaxAge()
                );

        if (!overlappingRules.isEmpty()) {
            throw new RuntimeException("Premium rule overlaps with an existing age range for this dependent type");
        }

        MembershipPlanPremiumRuleEntity entity = new MembershipPlanPremiumRuleEntity();
        entity.setPlan(plan);
        entity.setDependentType(dto.getDependentType());
        entity.setMinAge(dto.getMinAge());
        entity.setMaxAge(dto.getMaxAge());
        entity.setAdditionalPremiumCents(dto.getAdditionalPremiumCents());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(username);

        return toDto(premiumRuleRepository.save(entity));
    }

    @Transactional
    public MembershipPlanPremiumRuleResponseDto update(
            String planId,
            String ruleId,
            MembershipPlanPremiumRuleRequestDto dto,
            String username
    ) {
        MembershipPlanPremiumRuleEntity entity = premiumRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Premium rule not found"));

        if (!entity.getPlan().getId().equals(planId)) {
            throw new RuntimeException("Premium rule does not belong to this membership plan");
        }

        validate(dto);

        List<MembershipPlanPremiumRuleEntity> overlappingRules =
                premiumRuleRepository.findOverlappingRules(
                        planId,
                        dto.getDependentType(),
                        dto.getMinAge(),
                        dto.getMaxAge()
                );

        boolean overlapsAnotherRule = overlappingRules.stream()
                .anyMatch(rule -> !rule.getId().equals(ruleId));

        if (overlapsAnotherRule) {
            throw new RuntimeException("Premium rule overlaps with an existing age range for this dependent type");
        }

        entity.setDependentType(dto.getDependentType());
        entity.setMinAge(dto.getMinAge());
        entity.setMaxAge(dto.getMaxAge());
        entity.setAdditionalPremiumCents(dto.getAdditionalPremiumCents());
        entity.setActive(dto.getActive() != null ? dto.getActive() : entity.getActive());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(username);

        return toDto(premiumRuleRepository.save(entity));
    }

    public List<MembershipPlanPremiumRuleResponseDto> getByPlan(String planId) {
        return premiumRuleRepository.findByPlanIdAndActiveTrue(planId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Long resolveAdditionalPremiumCents(
            String planId,
            DependentType dependentType,
            Integer age
    ) {
        if (dependentType == null) {
            throw new RuntimeException("Dependent type is required to calculate additional premium");
        }

        if (age == null || age < 0) {
            throw new RuntimeException("Valid age is required to calculate additional premium");
        }

        return premiumRuleRepository
                .findFirstByPlanIdAndDependentTypeAndMinAgeLessThanEqualAndMaxAgeGreaterThanEqualAndActiveTrue(
                        planId,
                        dependentType,
                        age,
                        age
                )
                .map(MembershipPlanPremiumRuleEntity::getAdditionalPremiumCents)
                .orElse(0L);
    }

    @Transactional
    public void deactivate(String planId, String ruleId, String username) {
        MembershipPlanPremiumRuleEntity entity = premiumRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Premium rule not found"));

        if (!entity.getPlan().getId().equals(planId)) {
            throw new RuntimeException("Premium rule does not belong to this membership plan");
        }

        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(username);

        premiumRuleRepository.save(entity);
    }

    private void validate(MembershipPlanPremiumRuleRequestDto dto) {
        if (dto.getDependentType() == null) {
            throw new RuntimeException("Dependent type is required");
        }

        if (dto.getMinAge() == null || dto.getMinAge() < 0) {
            throw new RuntimeException("Minimum age must be zero or greater");
        }

        if (dto.getMaxAge() == null || dto.getMaxAge() < dto.getMinAge()) {
            throw new RuntimeException("Maximum age must be greater than or equal to minimum age");
        }

        if (dto.getAdditionalPremiumCents() == null || dto.getAdditionalPremiumCents() < 0) {
            throw new RuntimeException("Additional premium must be zero or greater");
        }
    }

    private MembershipPlanPremiumRuleResponseDto toDto(MembershipPlanPremiumRuleEntity entity) {
        return MembershipPlanPremiumRuleResponseDto.builder()
                .id(entity.getId())
                .planId(entity.getPlan().getId())
                .planName(entity.getPlan().getName())
                .dependentType(entity.getDependentType())
                .minAge(entity.getMinAge())
                .maxAge(entity.getMaxAge())
                .additionalPremiumCents(entity.getAdditionalPremiumCents())
                .active(entity.getActive())
                .build();
    }
}
