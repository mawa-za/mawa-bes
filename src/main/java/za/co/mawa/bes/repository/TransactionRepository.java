package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import za.co.mawa.bes.entity.TransactionEntity;
@Component
public interface TransactionRepository extends JpaRepository<TransactionEntity,String> {

}
