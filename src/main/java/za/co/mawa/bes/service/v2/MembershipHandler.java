package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.enums.ApprovalType;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.PartnerService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

public class MembershipHandler implements MembershipUpdateHandler {
    @Autowired
    MembershipService membershipService;
    @Autowired
    MembershipPlanService membershipPlanService;
    @Autowired
    MembershipDependentService membershipDependentService;
    @Autowired
    MembershipPlanPremiumRuleService membershipPlanPremiumRuleService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    MembershipRepository membershipRepository;

    @Override
    public void onUpdate(String id) {
        MembershipEntity membership = membershipService.getMembershipById(id).orElseThrow();
        Long totalPremiumCents = membershipPlanService.getPlanById(membership.getPlanId()).get().getPremiumCents();

        for (MembershipDependentEntity dependent : membershipDependentService.getDependentsByMembershipId(membership.getId())) {
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
        membership.setPremiumCents(totalPremiumCents);
        membership.setUpdatedBy(UserContext.getCurrentUserPartner());
        membership.setUpdatedAt(toLocalDateTime(new Date()));
        membershipRepository.save(membership);

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
}
