package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PartnerAttributeQueryDto implements Serializable
{
    private String partner;
    private String attribute;
    private String value;
    private Date validTo;
    private Date validFrom;
}
