package za.co.mawa.bes.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NotificationCreateDto implements Serializable {
    private String type;
    private String sender;
    private String recipient;
    private String subject;
    private String body;
    private String transactionId;
}
