package za.co.mawa.bes.dto.transaction.link;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;

import java.io.Serializable;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TransactionLinkInboundDto implements Serializable {
    private  String parent;
    private String child;
    private String type;
}
