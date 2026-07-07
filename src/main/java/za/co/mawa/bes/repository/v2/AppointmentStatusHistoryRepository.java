package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.AppointmentStatusHistoryEntity;

import java.util.List;

@Repository
public interface AppointmentStatusHistoryRepository extends JpaRepository<AppointmentStatusHistoryEntity, String> {
    List<AppointmentStatusHistoryEntity> findByAppointmentIdOrderByChangedAtDesc(String appointmentId);
}
