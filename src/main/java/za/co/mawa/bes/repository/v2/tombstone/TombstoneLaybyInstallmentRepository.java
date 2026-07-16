package za.co.mawa.bes.repository.v2.tombstone;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.tombstone.TombstoneLaybyInstallmentEntity;
import java.util.List;
import java.util.Optional;

public interface TombstoneLaybyInstallmentRepository extends JpaRepository<TombstoneLaybyInstallmentEntity, String> {
    List<TombstoneLaybyInstallmentEntity> findByLaybyAgreementIdOrderByInstallmentNoAsc(String laybyAgreementId);
}
