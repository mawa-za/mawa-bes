package za.co.mawa.bes.dto.transaction.link;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransactionLinkOutboundDto implements Serializable {
    private String parent;
    private String child;
    private String type;
}
