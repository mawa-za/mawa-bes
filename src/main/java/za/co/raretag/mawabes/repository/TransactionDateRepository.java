package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;
import za.co.raretag.mawabes.entity.TransactionDateEntity;
import za.co.raretag.mawabes.entity.TransactionDatePKEntity;

@Repository
public interface TransactionDateRepository extends JpaRepository<TransactionDateEntity, TransactionDatePKEntity> {
}
