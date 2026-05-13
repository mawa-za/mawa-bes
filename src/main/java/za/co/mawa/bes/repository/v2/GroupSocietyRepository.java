package za.co.mawa.bes.repository.v2;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;

import java.util.List;
import java.util.Optional;

public interface GroupSocietyRepository extends JpaRepository<GroupSocietyEntity, String> {

    Optional<GroupSocietyEntity> findByGroupNo(String groupNo);

    Optional<GroupSocietyEntity> findByPartnerId(String partnerId);

    boolean existsByGroupNo(String groupNo);

    boolean existsByPartnerId(String partnerId);

    List<GroupSocietyEntity> findByStatus(String status);

    List<GroupSocietyEntity> findBySocietyType(String societyType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GroupSocietyEntity g WHERE g.id = :id")
    Optional<GroupSocietyEntity> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GroupSocietyEntity g WHERE g.partnerId = :partnerId")
    Optional<GroupSocietyEntity> findByPartnerIdForUpdate(@Param("partnerId") String partnerId);
}