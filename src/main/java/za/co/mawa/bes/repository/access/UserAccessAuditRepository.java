package za.co.mawa.bes.repository.access;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.access.UserAccessAuditEntity;
public interface UserAccessAuditRepository extends JpaRepository<UserAccessAuditEntity,String> {}
