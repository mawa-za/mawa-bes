package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.MembershipChangeAuditEntity;
import java.util.List;
public interface MembershipChangeAuditRepository extends JpaRepository<MembershipChangeAuditEntity, String> {
    List<MembershipChangeAuditEntity> findByMembershipIdOrderByPerformedAtDesc(String membershipId);
}
