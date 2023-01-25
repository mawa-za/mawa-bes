package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerRoleEntity;
import za.co.mawa.bes.entity.PartnerRolePKEntity;

import java.util.List;

@Repository
public interface PartnerRoleRepository extends JpaRepository<PartnerRoleEntity, PartnerRolePKEntity> {
    @Query(value = "SELECT p FROM PartnerRole p WHERE p.partnerRolePK.role = :role", nativeQuery = true)
    List<PartnerRoleEntity> findPartnerByRole(String role);
}
