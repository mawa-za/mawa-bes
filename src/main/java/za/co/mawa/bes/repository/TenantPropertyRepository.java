package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.TenantPropertyEntity;
import za.co.mawa.bes.entity.TenantPropertyPKEntity;

@Repository
public interface TenantPropertyRepository extends JpaRepository<TenantPropertyEntity, TenantPropertyPKEntity> {

}
