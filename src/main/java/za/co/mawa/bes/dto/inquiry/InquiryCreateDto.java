package za.co.mawa.bes.dto.inquiry;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import za.co.mawa.bes.dto.LineItemDto;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class InquiryCreateDto implements Serializable {
private String customer;

    private List<LineItemDto> items;
}
