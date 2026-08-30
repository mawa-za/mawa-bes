package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.PartnerIdentityEntity;
import za.co.mawa.bes.dto.v2.MembershipDependentResponseDto;
import za.co.mawa.bes.mapper.v2.MembershipDependentMapper;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.PartnerIdentityRepository;
import za.co.mawa.bes.repository.v2.MembershipDependentRepository;
import za.co.mawa.bes.enums.MembershipDependentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MembershipDependentService {
    @Autowired
    MembershipUpdateHandlerRegistry membershipHandlerRegistry;

    private final MembershipDependentRepository membershipDependentRepository;
    private final PartnerRepository partnerRepository;
    private final MembershipDependentMapper membershipDependentMapper;
    private final PartnerIdentityRepository partnerIdentityRepository;

    @Autowired
    public MembershipDependentService(
            MembershipDependentRepository membershipDependentRepository,
            PartnerRepository partnerRepository,
            MembershipDependentMapper membershipDependentMapper,
            PartnerIdentityRepository partnerIdentityRepository) {
        this.membershipDependentRepository = membershipDependentRepository;
        this.partnerRepository = partnerRepository;
        this.membershipDependentMapper = membershipDependentMapper;
        this.partnerIdentityRepository = partnerIdentityRepository;
    }

    public List<MembershipDependentEntity> getDependentsByMembershipId(String membershipId) {
        return membershipDependentRepository.findByMembershipIdAndStatusInOrderByEffectiveFromAsc(
                membershipId,
                Set.of(MembershipDependentStatus.ACTIVE, MembershipDependentStatus.DECEASED)
        );
    }

    public List<MembershipDependentResponseDto> getDependentResponsesByMembershipId(String membershipId) {
        List<MembershipDependentEntity> dependents = getDependentsByMembershipId(membershipId);
        Map<String, PartnerEntity> partners = partnerRepository.findAllById(
                        dependents.stream()
                                .map(MembershipDependentEntity::getDependentPartnerId)
                                .filter(id -> id != null && !id.isBlank())
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(PartnerEntity::getId, Function.identity()));
        Map<String, PartnerIdentityEntity> identities = new java.util.LinkedHashMap<>();
        if (!partners.isEmpty()) {
            for (PartnerIdentityEntity identity : partnerIdentityRepository.findByPartnerIn(partners.keySet().stream().toList())) {
                if (identity == null || identity.getPartner() == null || identity.getPartnerIdentityPK() == null) continue;
                identities.merge(identity.getPartner(), identity, this::preferredIdentity);
            }
        }

        return dependents.stream()
                .map(dependent -> membershipDependentMapper.toResponse(
                        dependent,
                        partners.get(dependent.getDependentPartnerId()),
                        identities.get(dependent.getDependentPartnerId())))
                .toList();
    }

    private PartnerIdentityEntity preferredIdentity(PartnerIdentityEntity current, PartnerIdentityEntity candidate) {
        return identityPriority(candidate) < identityPriority(current) ? candidate : current;
    }

    private int identityPriority(PartnerIdentityEntity identity) {
        if (identity == null || identity.getPartnerIdentityPK() == null || identity.getPartnerIdentityPK().getType() == null) return 99;
        return "SA-ID".equalsIgnoreCase(identity.getPartnerIdentityPK().getType()) ? 0 : 10;
    }

    public MembershipDependentEntity addDependent(String membershipId, MembershipDependentEntity dependent) {
        dependent.setMembershipId(membershipId);
        dependent.setActive(true);
        dependent.setStatus(MembershipDependentStatus.ACTIVE);
        dependent.setEffectiveFrom(dependent.getEffectiveFrom() == null ? LocalDate.now() : dependent.getEffectiveFrom());
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
                    existingDependent.setStatus(Boolean.TRUE.equals(dependent.getActive())
                            ? MembershipDependentStatus.ACTIVE
                            : MembershipDependentStatus.REMOVED);
                    existingDependent.setEffectiveTo(Boolean.TRUE.equals(dependent.getActive())
                            ? null
                            : LocalDate.now());
                    return membershipDependentRepository.save(existingDependent);
                });

        membershipHandlerRegistry.handleUpdate(membershipId);
        return membershipDependentRepository.findById(dependentId);
    }

    public boolean deleteDependent(String membershipId, String dependentId) {
        Optional<MembershipDependentEntity> dependent = membershipDependentRepository.findById(dependentId)
                .filter(existingDependent -> existingDependent.getMembershipId().equals(membershipId));

        if (dependent.isPresent()) {
            MembershipDependentEntity existing = dependent.get();
            if (existing.getStatus() == MembershipDependentStatus.DECEASED) {
                throw new IllegalArgumentException("A deceased dependent cannot be removed from membership history");
            }
            existing.setActive(false);
            existing.setStatus(MembershipDependentStatus.REMOVED);
            existing.setEffectiveTo(LocalDate.now());
            membershipDependentRepository.save(existing);
            membershipHandlerRegistry.handleUpdate(membershipId);
            return true;
        }
        return false;
    }
}
