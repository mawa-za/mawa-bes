package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.GroupSocietyMemberEntity;

import java.util.List;
import java.util.Optional;

public interface GroupSocietyMemberRepository extends JpaRepository<GroupSocietyMemberEntity, String> {

    List<GroupSocietyMemberEntity> findByGroupSocietyId(String groupSocietyId);

    List<GroupSocietyMemberEntity> findByGroupSocietyIdAndStatus(String groupSocietyId, String status);

    Optional<GroupSocietyMemberEntity> findByGroupSocietyIdAndMemberId(String groupSocietyId, String memberId);

    boolean existsByGroupSocietyIdAndMemberId(String groupSocietyId, String memberId);

    void deleteByGroupSocietyId(String groupSocietyId);
}