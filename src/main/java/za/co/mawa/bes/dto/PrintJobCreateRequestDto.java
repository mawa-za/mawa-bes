package za.co.mawa.bes.dto;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobCreateRequestDto {

    private Timestamp creationTimestamp;
    private String printerId;
    private String content;
    private boolean completed;
    private Timestamp completedTimestamp;
}
