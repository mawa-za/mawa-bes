package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import za.co.raretag.mawabes.entity.TransactionEntity;

public interface TicketRepository extends JpaRepository<TransactionEntity,String> {


}
