package za.co.mawa.bes.dto.v2.appointment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AppointmentStatusUpdateRequest {
    private String status;
    private String reason;
}
