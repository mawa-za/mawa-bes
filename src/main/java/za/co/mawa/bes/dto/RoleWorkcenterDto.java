package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
@NoArgsConstructor
@Getter
@Setter
public class RoleWorkcenterDto implements Serializable {
    private String role;
    private WorkcenterDto workcenter;
    private int position;
}