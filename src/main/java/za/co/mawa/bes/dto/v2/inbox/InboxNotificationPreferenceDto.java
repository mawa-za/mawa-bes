package za.co.mawa.bes.dto.v2.inbox;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InboxNotificationPreferenceDto {
    private boolean popupEnabled = true;
    private LocalDateTime postponedUntil;
}
