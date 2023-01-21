package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.RoleWorkcenterEntity;
import za.co.mawa.bes.entity.RoleWorkcenterPKEntity;

@Repository
public interface RoleWorkcenterRepository extends JpaRepository<RoleWorkcenterEntity, RoleWorkcenterPKEntity> {
}
