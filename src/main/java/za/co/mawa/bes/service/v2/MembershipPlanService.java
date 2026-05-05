package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.repository.v2.MembershipPlanRepository;

import java.util.Optional;

@Service
public class MembershipPlanService {

    private final MembershipPlanRepository membershipPlanRepository;

    @Autowired
    public MembershipPlanService(MembershipPlanRepository membershipPlanRepository) {
        this.membershipPlanRepository = membershipPlanRepository;
    }

    public Page<MembershipPlanEntity> getAllPlans(Pageable pageable) {
        return membershipPlanRepository.findAll(pageable);
    }

    public Optional<MembershipPlanEntity> getPlanById(String id) {
        return membershipPlanRepository.findById(id);
    }

    public MembershipPlanEntity createPlan(MembershipPlanEntity membershipPlan) {
        return membershipPlanRepository.save(membershipPlan);
    }

    public Optional<MembershipPlanEntity> updatePlan(String id, MembershipPlanEntity membershipPlan) {
        return membershipPlanRepository.findById(id)
                .map(existingPlan -> {
                    existingPlan.setPlanCode(membershipPlan.getPlanCode());
                    existingPlan.setName(membershipPlan.getName());
                    existingPlan.setDescription(membershipPlan.getDescription());
                    existingPlan.setPremiumCents(membershipPlan.getPremiumCents());
                    existingPlan.setCurrency(membershipPlan.getCurrency());
                    existingPlan.setMaxDependents(membershipPlan.getMaxDependents());
                    existingPlan.setActive(membershipPlan.getActive());
                    return membershipPlanRepository.save(existingPlan);
                });
    }

    public boolean deletePlan(String id) {
        if (membershipPlanRepository.existsById(id)) {
            membershipPlanRepository.deleteById(id);
            return true;
        }
        return false;
    }
}