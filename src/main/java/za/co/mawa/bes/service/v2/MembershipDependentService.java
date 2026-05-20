package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.repository.v2.MembershipDependentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class MembershipDependentService {
    @Autowired
    MembershipUpdateHandlerRegistry membershipHandlerRegistry;

    private final MembershipDependentRepository membershipDependentRepository;

    @Autowired
    public MembershipDependentService(MembershipDependentRepository membershipDependentRepository) {
        this.membershipDependentRepository = membershipDependentRepository;
    }

    public List<MembershipDependentEntity> getDependentsByMembershipId(String membershipId) {
        return membershipDependentRepository.findByMembershipId(membershipId);
    }

    public MembershipDependentEntity addDependent(String membershipId, MembershipDependentEntity dependent) {
        dependent.setMembershipId(membershipId);
        MembershipDependentEntity newDependent = membershipDependentRepository.save(dependent);
        membershipHandlerRegistry.handleUpdate(membershipId);
        return membershipDependentRepository.findById(newDependent.getId()).orElseThrow();
    }

    public Optional<MembershipDependentEntity> updateDependent(String membershipId, String dependentId, MembershipDependentEntity dependent) {
        membershipDependentRepository.findById(dependentId)
                .filter(existingDependent -> existingDependent.getMembershipId().equals(membershipId))
                .map(existingDependent -> {
                    existingDependent.setDependentPartnerId(dependent.getDependentPartnerId());
                    existingDependent.setDependentType(dependent.getDependentType());
                    existingDependent.setActive(dependent.getActive());
                    return membershipDependentRepository.save(existingDependent);
                });

        membershipHandlerRegistry.handleUpdate(membershipId);
        return membershipDependentRepository.findById(dependentId);
    }

    public boolean deleteDependent(String membershipId, String dependentId) {
        Optional<MembershipDependentEntity> dependent = membershipDependentRepository.findById(dependentId)
                .filter(existingDependent -> existingDependent.getMembershipId().equals(membershipId));

        if (dependent.isPresent()) {
            membershipDependentRepository.deleteById(dependentId);
            membershipHandlerRegistry.handleUpdate(membershipId);
            return true;
        }
        return false;
    }
}