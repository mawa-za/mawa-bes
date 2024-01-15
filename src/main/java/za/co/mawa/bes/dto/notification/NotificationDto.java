package za.co.mawa.bes.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NotificationDto implements Serializable {
    private String id;

    private String transactionId;

    private String status;

    private String type;
    private String SubType;

    private String processor;

    private String recipient;

    private String statusReason;
    private String description;
    private String category;
    private String subDescription;

    private  String location;

    private ArrayList<NotificationDto> notificationDtos;

}
