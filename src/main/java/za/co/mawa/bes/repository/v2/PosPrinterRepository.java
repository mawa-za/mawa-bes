package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PosPrinterEntity;
import java.util.*;
public interface PosPrinterRepository extends JpaRepository<PosPrinterEntity,String>{ List<PosPrinterEntity> findByAgentIdOrderByDisplayNameAsc(String agentId); Optional<PosPrinterEntity> findByAgentIdAndWindowsQueueName(String agentId,String windowsQueueName); }
