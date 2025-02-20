package za.co.mawa.bes.dto.premium;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.membership.MembershipBasicDto;
import za.co.mawa.bes.dto.membership.MembershipDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.entity.PremiumEntity;
import za.co.mawa.bes.utils.Field;

import java.io.Serializable;
import java.math.BigDecimal;
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
    private PartnerDto createdBy;
    private PartnerDto employeeResponsible;
    private MembershipDto membership;
    private String membershipPeriod;
    private FieldOptionDto tenderType;
    private FieldOptionDto location;
    private BigDecimal amount;
    private String cashupId;
    private String terminalId;
    private String externalReceiptNo;

}
