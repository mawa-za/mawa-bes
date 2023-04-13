package za.co.mawa.bes.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@AllArgsConstructor
@Getter
@Setter
public class TransactionLinkDto implements Serializable {

    private  String transaction1;

    private String transaction2;

    private Date creationDate;
    private String createBy;
    private String type;

    public TransactionLinkDto() {

    }
}
