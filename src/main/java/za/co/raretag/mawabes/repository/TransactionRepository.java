package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import za.co.raretag.mawabes.entity.TransactionEntity;
@Component
public interface TransactionRepository extends JpaRepository<TransactionEntity,String> {

}
