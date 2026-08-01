package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.mawa.bes.entity.v2.AppointmentServiceOrderLinkEntity;

import java.util.Optional;

public interface AppointmentServiceOrderLinkRepository extends JpaRepository<AppointmentServiceOrderLinkEntity, String> {
    Optional<AppointmentServiceOrderLinkEntity> findByAppointmentId(String appointmentId);
    Optional<AppointmentServiceOrderLinkEntity> findByServiceOrderId(String serviceOrderId);
}
