package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPremiumEntity;
import za.co.mawa.bes.enums.PremiumStatus;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.repository.v2.MembershipDependentRepository;
import za.co.mawa.bes.repository.v2.MembershipPremiumRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.PartnerService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
public class MembershipHandler implements MembershipUpdateHandler {
    @Autowired
    MembershipPlanService membershipPlanService;
    @Autowired
    MembershipPlanPremiumRuleService membershipPlanPremiumRuleService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    MembershipRepository membershipRepository;
    @Autowired
    MembershipDependentRepository membershipDependentRepository;
    @Autowired
    MembershipPremiumRepository membershipPremiumRepository;

    @Override
    public void onUpdate(String id) {
        MembershipEntity membership = membershipRepository.findById(id).orElseThrow();
        Long totalPremiumCents = membershipPlanService.getPlanById(membership.getPlanId()).orElseThrow().getPremiumCents();

        for (MembershipDependentEntity dependent : membershipDependentRepository.findByMembershipId(membership.getId())) {
            if (dependent.getActive() != null && !dependent.getActive()) {
                continue;
            }
            try {
                PartnerDto partner = partnerService.get(dependent.getDependentPartnerId());
                Integer age = calculateAge(toLocalDate(partner.getBirthDate()));

                Long additionalPremiumCents =
                        membershipPlanPremiumRuleService.resolveAdditionalPremiumCents(
                                membership.getPlanId(),
                                dependent.getDependentType(),
                                age
                        );

                totalPremiumCents += additionalPremiumCents;
            } catch (PartnerNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        String userId = UserContext.getCurrentUserPartner();
        membership.setPremiumCents(totalPremiumCents);
        membership.setUpdatedBy(userId);
        membership.setUpdatedAt(toLocalDateTime(new Date()));
        membershipRepository.save(membership);
        recalculateOpenPremiums(membership.getId(), totalPremiumCents, userId);
    }

    private void recalculateOpenPremiums(String membershipId, Long premiumCents, String userId) {
        List<MembershipPremiumEntity> premiums = membershipPremiumRepository.findByMembershipIdAndStatusInOrderByPeriodYYYYMMAsc(
                membershipId,
                List.of(PremiumStatus.UNPAID, PremiumStatus.PARTIALLY_PAID)
        );

        for (MembershipPremiumEntity premium : premiums) {
            long paidAmount = safe(premium.getPaidAmountCents());
            long newAmount = safe(premiumCents);
            long newBalance = Math.max(0L, newAmount - paidAmount);

            premium.setAmountCents(newAmount);
            premium.setBalanceCents(newBalance);
            premium.setStatus(paidAmount <= 0 ? PremiumStatus.UNPAID : (newBalance <= 0 ? PremiumStatus.PAID : PremiumStatus.PARTIALLY_PAID));
            premium.setUpdatedBy(userId);
            premium.setUpdatedAt(LocalDateTime.now());
            membershipPremiumRepository.save(premium);
        }
    }

    public LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }

        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }

        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private Integer calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new RuntimeException("Date of birth is required to calculate premium");
        }

        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }
}
