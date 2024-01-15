package za.co.mawa.bes.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NotificationCreate1Dto {
    private String id;

    private String transactionId;

    private String status;

    private String type;
    private String SubType;

    private String processor;

    private String recipient;

    private String location;
}
