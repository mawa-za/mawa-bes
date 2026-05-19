package za.co.mawa.bes.dto.v2;

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
    private String referenceId;
    private String referenceNo;
}
