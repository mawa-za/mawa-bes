package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.TransactionDateEntity;
import za.co.mawa.bes.entity.TransactionDatePKEntity;

@Repository
public interface TransactionDateRepository extends JpaRepository<TransactionDateEntity, TransactionDatePKEntity> {
}
