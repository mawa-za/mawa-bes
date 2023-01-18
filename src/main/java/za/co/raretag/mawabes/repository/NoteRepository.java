package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.raretag.mawabes.entity.TransactionEntity;

public interface NoteRepository extends JpaRepository<TransactionEntity,String> {
}
