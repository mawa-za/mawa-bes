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
             ORDER BY f.createdAt DESC
            """)
    List<FuneralServiceEntity> search(@Param("query") String query, @Param("status") String status);
}

