package za.co.mawa.bes.dto.v2.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueueAdminListResponse {
    private List<MessageQueueAdminDto> items;
    private long total;
}
