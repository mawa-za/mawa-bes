package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.TransactionPartnerEntity;
import za.co.mawa.bes.entity.TransactionPartnerPKEntity;

@Repository
public interface TransactionPartnerRepository  extends JpaRepository<TransactionPartnerEntity, TransactionPartnerPKEntity> {
}
