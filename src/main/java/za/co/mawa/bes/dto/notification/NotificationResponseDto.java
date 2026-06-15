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
public class NotificationResponseDto {

    private String id;
    private String type;
    private String sender;
    private String recipient;
    private String subject;
    private String body;
    private String created_by;
    private Date createdAt;
    private String status;
}
