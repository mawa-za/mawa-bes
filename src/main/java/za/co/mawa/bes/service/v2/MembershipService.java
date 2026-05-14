package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.exception.NumberRangeObjectNotFound;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.NumberRangeService;
import za.co.mawa.bes.utils.TransactionType;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.criteria.Predicate;

@Service(value = "MembershipServiceV2")
public class MembershipService {

    private final MembershipRepository membershipRepository;
    @Autowired
    NumberRangeService numberRangeService;

    @Autowired
    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Page<MembershipEntity> getAllMemberships(Pageable pageable) {
        return membershipRepository.findAll(pageable);
    }

    public Page<MembershipEntity> getMembershipsByMemberId(List<String> memberIds, Pageable pageable) {
        Specification<MembershipEntity> spec = (root, queryObj, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Add query filter (if provided)
//            if (query != null && !query.isEmpty()) {
//                predicates.add(
//                        criteriaBuilder.like(root.get("membershipNo"), "%" + query + "%")
//                );
//            }

            // Add memberId filters (if provided)
            if (memberIds != null && !memberIds.isEmpty()) {
                predicates.add(root.get("memberId").in(memberIds));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return membershipRepository.findAll(spec, pageable);
    }

    public Optional<MembershipEntity> getMembershipById(String id) {
        return membershipRepository.findById(id);
    }

    public MembershipEntity createMembership(MembershipEntity membership) {
        try {
            String id = numberRangeService.generateNumber(TransactionType.MEMBERSHIP);
            membership.setCreatedAt(new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            membership.setCreatedBy(UserContext.getCurrentUserPartner());
            membership.setMembershipNo(id);
            return membershipRepository.save(membership);
        } catch (NumberRangeObjectNotFound e) {
            throw new RuntimeException(e);
        }

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