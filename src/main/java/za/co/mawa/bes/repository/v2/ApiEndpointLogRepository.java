package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.ApiEndpointLogEntity;

import java.util.Optional;

@Repository
public interface ApiEndpointLogRepository extends JpaRepository<ApiEndpointLogEntity, String> {

    @Query(value = "SELECT * "
            + "FROM api_endpoint_log "
            + "WHERE direction = 'OUTBOUND' "
            + "AND integration_name = 'FNB' "
            + "AND method = 'POST' "
            + "AND endpoint LIKE '%/paymentExecution/initiate/v1%' "
            + "AND success = TRUE "
            + "AND status_code BETWEEN 200 AND 299 "
            + "AND request_body LIKE CONCAT('%', :paymentReference, '%') "
            + "AND response_body LIKE '%instructionId%' "
            + "ORDER BY created_at DESC "
            + "LIMIT 1", nativeQuery = true)
    Optional<ApiEndpointLogEntity> findLatestSuccessfulFnbInitiateByPaymentReference(
            @Param("paymentReference") String paymentReference
    );
}
