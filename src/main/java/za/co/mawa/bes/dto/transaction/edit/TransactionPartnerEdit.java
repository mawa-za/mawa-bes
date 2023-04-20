package za.co.mawa.bes.dto.transaction.edit;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TransactionPartnerEdit implements Serializable {

    private String partnerFunction;
    private String parnter;
    private String transaction;

}
