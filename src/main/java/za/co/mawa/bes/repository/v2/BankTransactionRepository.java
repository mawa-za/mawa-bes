package za.co.mawa.bes.repository.v2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import za.co.mawa.bes.entity.v2.BankTransactionEntity;
import java.time.LocalDate;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransactionEntity, Long> {
    boolean existsByFingerprint(String fingerprint);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from BankTransactionEntity t where t.id = :id")
    Optional<BankTransactionEntity> findLocked(@Param("id") Long id);

    @Query("select t from BankTransactionEntity t where "
            + "(:search = '' or lower(t.referenceText) like lower(concat('%', :search, '%')) "
            + "or t.accountNumber like concat('%', :search, '%') "
            + "or (cast(t.id as string) = :search)) "
            + "and (:creditsOnly = false or t.amountCents > 0) "
            + "and (:fromDate is null or t.transactionDate >= :fromDate) "
            + "and (:toDate is null or t.transactionDate <= :toDate) "
            + "and (:amountCents is null or t.amountCents = :amountCents)")
    Page<BankTransactionEntity> search(@Param("search") String search,
                                       @Param("creditsOnly") boolean creditsOnly,
                                       @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                       @Param("amountCents") Long amountCents, Pageable pageable);

    /** Credit-only picker search: never expose spent funds as selectable deposits. */
    @Query("select t from BankTransactionEntity t where t.amountCents > 0 "
            + "and t.amountCents > (select coalesce(sum(a.amountCents), 0) "
            + "from BankTransactionAllocationEntity a where a.bankTransactionId = t.id and a.releasedAt is null) "
            + "and (lower(t.referenceText) like lower(concat('%', :search, '%')) escape '!' "
            + "or t.accountNumber like concat('%', :search, '%') escape '!' "
            + "or cast(t.id as string) = :search)")
    Page<BankTransactionEntity> searchAvailableCredits(@Param("search") String search, Pageable pageable);

    /** Exact full Deposit ID lookup; a debit or exhausted credit can never be picked. */
    @Query("select t from BankTransactionEntity t where t.id = :id and t.amountCents > 0 "
            + "and t.amountCents > (select coalesce(sum(a.amountCents), 0) "
            + "from BankTransactionAllocationEntity a where a.bankTransactionId = t.id and a.releasedAt is null)")
    Page<BankTransactionEntity> findAvailableCreditById(@Param("id") Long id, Pageable pageable);

    /** Exact ID search avoids matching digits inside another transaction's free-text reference. */
    @Query("select t from BankTransactionEntity t where t.id = :id "
            + "and (:creditsOnly = false or t.amountCents > 0) "
            + "and (:fromDate is null or t.transactionDate >= :fromDate) "
            + "and (:toDate is null or t.transactionDate <= :toDate) "
            + "and (:amountCents is null or t.amountCents = :amountCents)")
    Page<BankTransactionEntity> searchById(@Param("id") Long id,
                                           @Param("creditsOnly") boolean creditsOnly,
                                           @Param("fromDate") LocalDate fromDate,
                                           @Param("toDate") LocalDate toDate,
                                           @Param("amountCents") Long amountCents, Pageable pageable);
}
