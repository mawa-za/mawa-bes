package za.co.mawa.bes.fnb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class GroupHeader implements Serializable {
    private String messageId;
    private String creationDateTime;
    private String initiatingPartyName;
    private String initiatingPartyBIC;
    private int totalNumberOfTransactions;
    private double totalControlSum;
}