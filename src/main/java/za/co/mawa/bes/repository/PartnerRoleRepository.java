package za.co.mawa.bes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerRoleEntity;
import za.co.mawa.bes.entity.PartnerRolePKEntity;

import java.util.List;

@Repository
public interface PartnerRoleRepository extends JpaRepository<PartnerRoleEntity, PartnerRolePKEntity> {
    @Query("SELECT p FROM PartnerRoleEntity p WHERE p.partnerRolePK.role = :role")
    List<PartnerRoleEntity> findPartnerByRole(String role);

    @Query("SELECT p FROM PartnerRoleEntity p WHERE p.partnerRolePK.role = :role")
    List<PartnerRoleEntity> findPartnerByRole2(String role);

    @Query("SELECT p FROM PartnerRoleEntity p WHERE p.partnerRolePK.id = :id")
    List<PartnerRoleEntity> findRoleByPartner(String id);

    @Query("SELECT p FROM PartnerRoleEntity p WHERE p.partnerRolePK.role = :role")
    Page<PartnerRoleEntity> findPartnerByRolePage(String role , Pageable pageable);
}
