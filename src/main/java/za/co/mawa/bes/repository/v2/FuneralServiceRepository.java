package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.FuneralServiceEntity;

@Repository
public interface FuneralServiceRepository extends JpaRepository<FuneralServiceEntity, String> {
}
