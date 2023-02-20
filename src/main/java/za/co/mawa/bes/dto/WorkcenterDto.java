package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class WorkcenterDto implements Serializable {
    private String id;
    private String description;
    private String defaultFunction;
    private String path;

    public WorkcenterDto(String id, String description, String defaultFunction) {
        this.id = id;
        this.description = description;
        this.defaultFunction = defaultFunction;
        if (defaultFunction != "" && defaultFunction != null) {
            this.path = id + "-" + defaultFunction;
        }else{
            this.path = id;
        }
    }

}
