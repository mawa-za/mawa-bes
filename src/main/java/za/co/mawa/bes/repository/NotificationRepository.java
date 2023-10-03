package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.AddressEntity;
import za.co.mawa.bes.entity.notification.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
}