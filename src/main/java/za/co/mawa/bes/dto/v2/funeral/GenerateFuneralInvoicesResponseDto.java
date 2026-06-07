package za.co.mawa.bes.dto.v2.funeral;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GenerateFuneralInvoicesResponseDto {
    private String funeralServiceId;
    private List<String> invoiceIds;
}
