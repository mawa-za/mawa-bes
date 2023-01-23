package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerRoleEntity;
import za.co.mawa.bes.entity.PartnerRolePKEntity;
@Repository
public interface PartnerRoleRepository extends JpaRepository<PartnerRoleEntity, PartnerRolePKEntity> {
}
