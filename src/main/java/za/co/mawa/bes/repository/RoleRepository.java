package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.entity.RoleEntity;
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, String> {
    @Query("SELECT p FROM RoleEntity p WHERE p.id = :id ORDER BY p.id")
    RoleEntity getById(String id);
}
