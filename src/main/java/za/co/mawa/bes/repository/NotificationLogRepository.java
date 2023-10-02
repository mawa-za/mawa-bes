package za.co.mawa.bes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.notification.NotificationEntity;
import za.co.mawa.bes.entity.notification.NotificationLogEntity;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, String> {
}