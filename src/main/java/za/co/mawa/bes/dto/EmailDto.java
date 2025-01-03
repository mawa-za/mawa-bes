package za.co.mawa.bes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EmailDto implements Serializable {
    private String to;
    private String subject;
    private String template;
    private List<File> files = new ArrayList<>();
    private List<PropertyDto> properties;

}
