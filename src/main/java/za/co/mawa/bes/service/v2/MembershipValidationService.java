package za.co.mawa.bes.service.v2;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.repository.v2.MembershipPlanRepository;

@Service
public class MembershipValidationService {

    private final MembershipPlanRepository membershipPlanRepository;

    public MembershipValidationService(MembershipPlanRepository membershipPlanRepository) {
        this.membershipPlanRepository = membershipPlanRepository;
    }

    public void validateMembership(MembershipEntity membership) {
        // Validate that the provided plan exists
        String planId = membership.getPlanId();
        if (!membershipPlanRepository.existsById(planId)) {
            throw new IllegalArgumentException("Invalid planId: " + planId);
        }

        // Add additional validation logic
    }
}