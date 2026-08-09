package za.co.mawa.bes.repository.v2;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PosPrintAgentEntity;
import java.util.List;
public interface PosPrintAgentRepository extends JpaRepository<PosPrintAgentEntity,String>{ List<PosPrintAgentEntity> findAllByOrderByNameAsc(); }
