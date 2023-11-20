package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class PartnerBankAccountGetDto implements Serializable {

    private  String partner;
    private List<PartnerBankAccountDto> partnerBankAccountDtoList;

}
