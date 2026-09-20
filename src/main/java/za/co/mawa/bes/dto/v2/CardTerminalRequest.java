package za.co.mawa.bes.dto.v2;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class CardTerminalRequest {
    private String code;
    private String name;
    private String merchantNumber;
    private String locationCode;
    private Boolean active;
    private String changedBy;
}
