package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PartnerResourceApiEntity;

import java.util.List;
@Repository
public interface PartnerResourceApiRepository extends JpaRepository<PartnerResourceApiEntity,String> {
    @Query(value = "SELECT r FROM PartnerResourceApi r WHERE r.partner_no = :partner_no",nativeQuery = true)
    List<PartnerResourceApiEntity> findByPartner(String partner_no);
}
