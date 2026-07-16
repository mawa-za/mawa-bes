package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneLaybyAgreementEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneLaybyAgreementRepository extends JpaRepository<TombstoneLaybyAgreementEntity, String> {
    Optional<TombstoneLaybyAgreementEntity> findByTombstoneOrderId(String tombstoneOrderId);
    Optional<TombstoneLaybyAgreementEntity> findByAgreementNo(String agreementNo);
    List<TombstoneLaybyAgreementEntity> findAllByOrderByCreatedAtDesc();
    List<TombstoneLaybyAgreementEntity> findByStatusOrderByCreatedAtDesc(String status);
}
