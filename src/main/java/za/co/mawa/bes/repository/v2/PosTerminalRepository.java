package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.PosTerminalEntity;

import java.util.List;
import java.util.Optional;

public interface PosTerminalRepository extends JpaRepository<PosTerminalEntity, String> {
    Optional<PosTerminalEntity> findByTerminalKey(String terminalKey);
    List<PosTerminalEntity> findAllByOrderByDisplayNameAsc();
}
