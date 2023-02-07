package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class VersionDto {
    private String appName;
    private String versionNumber;
    private boolean appUsable;
    private Date validFrom;
    private Date validTo;
}
