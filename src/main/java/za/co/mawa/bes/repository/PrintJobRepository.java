package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.PrintJobEntity;

import java.util.List;

@Repository
public interface PrintJobRepository extends JpaRepository<PrintJobEntity, Long> {
    // You can add custom query methods here if needed
    List<PrintJobEntity> findByCompletedAndPrinterId(boolean completed, String printerId);
}
