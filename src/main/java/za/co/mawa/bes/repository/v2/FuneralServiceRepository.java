package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.FuneralServiceEntity;

@Repository
public interface FuneralServiceRepository extends JpaRepository<FuneralServiceEntity, String> {

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
              FROM FuneralServiceEntity f
             WHERE f.mortuaryInventoryId = :mortuaryInventoryId
               AND UPPER(COALESCE(f.status, '')) <> 'CANCELLED'
            """)
    boolean existsActiveByMortuaryInventoryId(@Param("mortuaryInventoryId") String mortuaryInventoryId);

    @Query("""
            SELECT f
              FROM FuneralServiceEntity f
             WHERE (:status IS NULL OR :status = '' OR UPPER(f.status) = UPPER(:status))
               AND (:query IS NULL OR :query = ''
                    OR LOWER(f.deceasedName) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(f.serviceRequestNo, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(f.deceasedIdentityNumber, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(f.funeralArea, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(f.deathCertificateNo, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(f.causeOfDeath, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(COALESCE(f.status, '')) LIKE LOWER(CONCAT('%', :query, '%')))
             ORDER BY CASE WHEN f.membershipNo IS NULL OR f.membershipNo = '' THEN 1 ELSE 0 END,
                      f.membershipNo DESC,
                      f.serviceRequestNo DESC
            """)
    List<FuneralServiceEntity> search(@Param("query") String query, @Param("status") String status);
}

