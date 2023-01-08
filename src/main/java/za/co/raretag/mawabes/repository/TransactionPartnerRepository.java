package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.raretag.mawabes.entity.TransactionPartnerEntity;
import za.co.raretag.mawabes.entity.TransactionPartnerPKEntity;

@Repository
public interface TransactionPartnerRepository  extends JpaRepository<TransactionPartnerEntity, TransactionPartnerPKEntity> {
}
