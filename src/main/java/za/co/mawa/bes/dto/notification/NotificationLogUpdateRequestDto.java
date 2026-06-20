package za.co.mawa.bes.dto.notification;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogUpdateRequestDto {

    private Integer id;
    private String notificationId;
    private String action;
    private String result;
    private Date executedAt;
}
