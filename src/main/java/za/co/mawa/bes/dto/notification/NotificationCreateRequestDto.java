package za.co.mawa.bes.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequestDto {

    private String type;
    private String sender;
    private String recipient;
    private String subject;
    private String body;
    private String created_by;
    private String status;
}
