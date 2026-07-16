package za.co.mawa.bes.repository.security;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.security.PasswordResetTokenEntity;

import java.util.Date;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetTokenEntity token where token.tokenHash = :tokenHash")
    Optional<PasswordResetTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update PasswordResetTokenEntity token set token.consumedAt = :consumedAt "
            + "where token.userId = :userId and token.consumedAt is null")
    int consumeActiveTokensForUser(
            @Param("userId") String userId,
            @Param("consumedAt") Date consumedAt
    );

    @Query("select count(token) from PasswordResetTokenEntity token "
            + "where lower(token.requestedEmail) = lower(:email) and token.requestedAt >= :since")
    long countRecentForEmail(@Param("email") String email, @Param("since") Date since);

    @Query("select count(token) from PasswordResetTokenEntity token "
            + "where token.requestIp = :requestIp and token.requestedAt >= :since")
    long countRecentForIp(@Param("requestIp") String requestIp, @Param("since") Date since);
}
