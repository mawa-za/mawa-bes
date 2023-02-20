package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.RoleWorkcenterEntity;
import za.co.mawa.bes.entity.RoleWorkcenterPKEntity;
import za.co.mawa.bes.entity.UserRoleEntity;

import java.util.List;

@Repository
public interface RoleWorkcenterRepository extends JpaRepository<RoleWorkcenterEntity, RoleWorkcenterPKEntity> {
    @Query("SELECT r FROM RoleWorkcenterEntity r WHERE r.roleWorkcenterPKEntity.role = :role")
    List<RoleWorkcenterEntity> findRoleWorkcenters(@Param("role") String role);
}
