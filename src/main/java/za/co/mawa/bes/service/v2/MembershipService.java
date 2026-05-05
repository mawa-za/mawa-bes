package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.util.Optional;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    @Autowired
    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Page<MembershipEntity> getAllMemberships(Pageable pageable) {
        return membershipRepository.findAll(pageable);
    }

    public Optional<MembershipEntity> getMembershipById(String id) {
        return membershipRepository.findById(id);
    }

    public MembershipEntity createMembership(MembershipEntity membership) {
        return membershipRepository.save(membership);
    }

    public Optional<MembershipEntity> updateMembership(String id, MembershipEntity membership) {
        return membershipRepository.findById(id)
                .map(existingMembership -> {
                    existingMembership.setMemberId(membership.getMemberId());
                    existingMembership.setMembershipNo(membership.getMembershipNo());
                    existingMembership.setPlanId(membership.getPlanId());
                    existingMembership.setStartDate(membership.getStartDate());
                    existingMembership.setEndDate(membership.getEndDate());
                    existingMembership.setStatus(membership.getStatus());
                    existingMembership.setPaidUpToPeriod(membership.getPaidUpToPeriod());
                    existingMembership.setJoinDate(membership.getJoinDate());
                    return membershipRepository.save(existingMembership);
                });
    }

    public boolean deleteMembership(String id) {
        if (membershipRepository.existsById(id)) {
            membershipRepository.deleteById(id);
            return true;
        }
        return false;
    }
}