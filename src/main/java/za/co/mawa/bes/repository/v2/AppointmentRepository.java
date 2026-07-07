package za.co.mawa.bes.repository.v2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import za.co.mawa.bes.entity.v2.AppointmentEntity;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, String>, JpaSpecificationExecutor<AppointmentEntity> {
    boolean existsByAppointmentNo(String appointmentNo);
}
