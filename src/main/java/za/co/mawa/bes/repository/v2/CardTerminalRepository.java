package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.CardTerminalEntity;
import java.util.List;
public interface CardTerminalRepository extends JpaRepository<CardTerminalEntity,String> {
    List<CardTerminalEntity> findAllByOrderByNameAsc();
    List<CardTerminalEntity> findByActiveTrueOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}
