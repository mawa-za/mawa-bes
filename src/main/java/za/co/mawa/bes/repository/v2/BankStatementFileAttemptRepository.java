package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.BankStatementFileAttemptEntity;
import java.util.List;

public interface BankStatementFileAttemptRepository extends JpaRepository<BankStatementFileAttemptEntity, Long> {
    List<BankStatementFileAttemptEntity> findByFileIdOrderByStartedAtDescIdDesc(Long fileId);
}
