package za.co.mawa.bes.repository.v2;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.v2.PosPrintJobEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
public interface PosPrintJobRepository extends JpaRepository<PosPrintJobEntity,String>{
 Optional<PosPrintJobEntity> findByIdempotencyKey(String key);
 List<PosPrintJobEntity> findTop100ByOrderByCreatedAtDesc();
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select j from PosPrintJobEntity j where j.agentId=:agentId and j.status='QUEUED' and j.attemptCount<j.maxAttempts and (j.nextAttemptAt is null or j.nextAttemptAt<=:now) order by j.priority desc,j.createdAt asc")
 List<PosPrintJobEntity> findClaimable(@Param("agentId") String agentId,@Param("now") LocalDateTime now,Pageable pageable);
 @Modifying @Query("update PosPrintJobEntity j set j.status='QUEUED',j.claimToken=null,j.claimedByAgentId=null,j.claimedAt=null,j.claimExpiresAt=null,j.updatedAt=:now where j.agentId=:agentId and j.status='CLAIMED' and j.attemptCount<j.maxAttempts and j.claimExpiresAt<:now")
 int releaseExpired(@Param("agentId") String agentId,@Param("now") LocalDateTime now);
 @Modifying @Query("update PosPrintJobEntity j set j.status='FAILED',j.failedAt=:now,j.lastError='Claim lease expired after maximum attempts',j.claimToken=null,j.claimedByAgentId=null,j.claimedAt=null,j.claimExpiresAt=null,j.updatedAt=:now where j.agentId=:agentId and j.status='CLAIMED' and j.attemptCount>=j.maxAttempts and j.claimExpiresAt<:now")
 int failExhaustedExpired(@Param("agentId") String agentId,@Param("now") LocalDateTime now);
 @Modifying @Query("update PosPrintJobEntity j set j.status='FAILED',j.failedAt=:now,j.lastError=:reason,j.claimToken=null,j.claimedByAgentId=null,j.claimedAt=null,j.claimExpiresAt=null,j.updatedAt=:now where j.agentId=:agentId and j.status in ('QUEUED','CLAIMED')")
 int failOpenForAgent(@Param("agentId") String agentId,@Param("now") LocalDateTime now,@Param("reason") String reason);

}
