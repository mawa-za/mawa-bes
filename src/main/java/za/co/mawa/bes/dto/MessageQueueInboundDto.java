package za.co.mawa.bes.dto;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class MessageQueueInboundDto implements Serializable {
    private String type;
    private String payload;
}
