package za.co.mawa.bes.dto.v2;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuneralPickupRequestResponseDto {

    private String id;
    private String deceasedName;
    private String pickupLocation;
    private String contactPerson;
    private String contactNumber;
    private String assignedStaffId;
    private LocalDateTime completionTime;
    private String status;
    private String mortuaryInventoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
