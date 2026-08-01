package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import za.co.mawa.bes.entity.v2.AppointmentServiceOrderEntity;

import java.util.Optional;

public interface AppointmentServiceOrderRepository extends
        JpaRepository<AppointmentServiceOrderEntity, String>,
        JpaSpecificationExecutor<AppointmentServiceOrderEntity> {
    Optional<AppointmentServiceOrderEntity> findByAppointmentId(String appointmentId);
    boolean existsByServiceOrderNo(String serviceOrderNo);
}
