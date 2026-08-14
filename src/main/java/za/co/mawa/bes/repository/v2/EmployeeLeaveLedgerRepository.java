package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.EmployeeLeaveLedgerEntity;
import java.util.List;
import java.util.Optional;
@Repository
public interface EmployeeLeaveLedgerRepository extends JpaRepository<EmployeeLeaveLedgerEntity, String> {
    List<EmployeeLeaveLedgerEntity> findByEmploymentIdOrderByTransactionDateDescCreatedAtDesc(String employmentId);
    Optional<EmployeeLeaveLedgerEntity> findByReferenceTypeAndReferenceIdAndTransactionType(String referenceType, String referenceId, String transactionType);
    Optional<EmployeeLeaveLedgerEntity> findByEmploymentIdAndReferenceTypeAndReferenceIdAndTransactionType(
            String employmentId, String referenceType, String referenceId, String transactionType);
}
