package za.co.mawa.bes.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.partner.PartnerDto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NotificationDto implements Serializable {
    private String id;

    private String transactionId;
    private String status;
    private FieldOptionDto type;
    private String SubType;
    private PartnerDto processor;
    private PartnerDto recipient;
    private String statusReason;
    private String description;
    private String category;
    private String subDescription;
    private String location;
    private ArrayList<NotificationDto> notificationDto;
    private Date creationDate;
    private Date lastModifiedDate;
}
