package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.MawaPayDeviceIdentityEntity;

import java.util.Optional;

public interface MawaPayDeviceIdentityRepository extends JpaRepository<MawaPayDeviceIdentityEntity, String> {
    Optional<MawaPayDeviceIdentityEntity> findByDeviceId(String deviceId);
}
