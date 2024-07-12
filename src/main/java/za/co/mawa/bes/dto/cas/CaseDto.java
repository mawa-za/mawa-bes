package za.co.mawa.bes.dto.cas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.AmountDto;
import za.co.mawa.bes.dto.DateDto;
import za.co.mawa.bes.dto.FieldOptionDto;
import za.co.mawa.bes.dto.comment.CommentDto;
import za.co.mawa.bes.dto.participant.ParticipantDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductBasicDto;
import za.co.mawa.bes.dto.task.TaskDto;
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
    private String number;
    private String description;
    private FieldOptionDto type;
    private PartnerDto client;
    private FieldOptionDto court;
    private FieldOptionDto status;
    private FieldOptionDto statusReason;
    private FieldOptionDto caseType;
    private ProductBasicDto product;
    private List<ParticipantDto> participants = new ArrayList<>();
    private List<PartnerDto> applicants = new ArrayList<>();
    private List<PartnerDto> defendants = new ArrayList<>();
    private List<DateDto> dates  = new ArrayList<>();
    private List<TaskDto> tasks  = new ArrayList<>();
    private List<CommentDto> comments  = new ArrayList<>();
    private PartnerDto createdBy;
}
