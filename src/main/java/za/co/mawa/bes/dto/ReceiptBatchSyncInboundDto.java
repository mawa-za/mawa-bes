package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReceiptBatchSyncInboundDto implements Serializable {
    private String deviceId;
    private List<ReceiptSyncItemInboundDto> items;
}
