package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CaseNoteEntity;
import java.util.List;

public interface CaseNoteRepository extends JpaRepository<CaseNoteEntity, String> {
    List<CaseNoteEntity> findByCaseIdOrderByCreatedAtDesc(String caseId);
}
