package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.SettingPKEntity;
import za.co.mawa.bes.entity.SettingEntity;

@Repository
public interface SettingRepository extends JpaRepository<SettingEntity, SettingPKEntity>{
}
