package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.GroupSocietyContactEntity;

import java.util.List;

public interface GroupSocietyContactRepository extends JpaRepository<GroupSocietyContactEntity, String> {

    List<GroupSocietyContactEntity> findByGroupSocietyId(String groupSocietyId);

    List<GroupSocietyContactEntity> findByGroupSocietyIdAndPrimaryContactTrue(String groupSocietyId);

    void deleteByGroupSocietyId(String groupSocietyId);
}