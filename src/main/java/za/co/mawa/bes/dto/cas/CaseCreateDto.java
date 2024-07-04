package za.co.mawa.bes.dto.cas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CaseCreateDto implements Serializable {
    private String client;
    private String type;
    //
    //private String caseType;
    //private String courtName;
    private String product;
    private String court;
    private String description;
    private List<String> applicants;
    private List<String> defendants;
}
