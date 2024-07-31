package za.co.mawa.bes.dto.claim;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.BankAccountDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.contract.ContractDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class ClaimOutboundDto implements Serializable {
    private String id;
    private String number;
    private String description;
    private FieldOptionDto type;
    private MembershipDto membership;
    private TransactionDto parent;
    private PartnerDto member;
    private PartnerDto customer;
    private PartnerDto claimant;
    private PartnerDto informant;
    private PartnerDto deceased;
    private Date deathDate;
    private Date burialDate;
    private Date creationDate;
    private FieldOptionDto paymentMethod;
    private FieldOptionDto branch;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private BankAccountDto bankDetails;

}
