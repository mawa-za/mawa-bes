package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@Getter
@Setter
public class DependentDto implements Serializable {
    private String id;
    private String title;
    private String idType;
    private String idNumber;
    private String name1;
    private String name2;
    private String name3;
    private String gender;
    private Date dateAdded;
    private Date dateEffective;
    private String status;
}
