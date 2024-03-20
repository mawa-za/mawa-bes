package za.co.mawa.bes.dto.cas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.AmountDto;
import za.co.mawa.bes.dto.DateDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.participant.ParticipantDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductBasicDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CaseDto {
    private String id;
    private FieldOptionDto type;
    private String number;
    private PartnerDto client;
    private ProductBasicDto service;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private List<ParticipantDto> participants = new ArrayList<>();
    private List<DateDto> dates  = new ArrayList<>();
}
