package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class TransactionCreateDto {
    private String number;
    private String customer;
    private String member;
    private String supplier;
    private String claimant;
    private String description;
    private String type;
    private String subType;
    private String status;
}
