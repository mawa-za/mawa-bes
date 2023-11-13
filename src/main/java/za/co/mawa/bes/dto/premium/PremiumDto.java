package za.co.mawa.bes.dto.premium;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.entity.PremiumEntity;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PremiumDto implements Serializable {
    private String id;
    private String receiptNumber;
    private String creationDate;
    private String creationTime;
    private String employeeResponsible;
    private String membershipId;
    private String membershipPeriod;
    private String tenderType;
    private String location;
    private String amount;
    private String cashupId;
    private String terminalId;

    public PremiumDto(PremiumEntity premiumEntity){
        SimpleDateFormat formatterTime = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd");
        this.setId(premiumEntity.getId());
        this.setReceiptNumber(premiumEntity.getReceiptNumber());
        this.setMembershipId(premiumEntity.getMembershipId());
        this.setMembershipPeriod(premiumEntity.getMembershipPeriod());
        this.setAmount(premiumEntity.getAmount().toString());
        this.setTenderType(premiumEntity.getTenderType());
        this.setEmployeeResponsible(premiumEntity.getCreatedBy());
        this.setLocation(premiumEntity.getLocation());
        this.setCreationDate(formatterDate.format(premiumEntity.getCreationDate()));
        this.setCreationTime(formatterTime.format(premiumEntity.getCreationTime()));
    }
}
