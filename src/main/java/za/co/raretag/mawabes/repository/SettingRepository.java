package za.co.raretag.mawabes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.raretag.mawabes.entity.SettingEntity;
import za.co.raretag.mawabes.entity.SettingPKEntity;
@Repository
public interface SettingRepository extends JpaRepository<SettingEntity, SettingPKEntity>{
}
