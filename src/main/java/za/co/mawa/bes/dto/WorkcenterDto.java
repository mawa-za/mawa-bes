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
    private String routeKey;
    private String routePath;
    private String iconKey;
    private String groupCode;
    private String groupTitle;
    private String groupDescription;
    private String sectionCode;
    private String sectionTitle;
    private Integer sectionDisplayOrder;
    private Integer groupDisplayOrder;
    private Integer displayOrder;
    private String cardDescription;
    private Boolean active;
    private Boolean assignable;
    private String permissionCode;

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

    public static WorkcenterDto catalogued(String id, String description, String defaultFunction,
            String routeKey, String routePath, String iconKey, String groupCode,
            String groupTitle, String groupDescription, String sectionCode, String sectionTitle,
            Integer sectionDisplayOrder, Integer groupDisplayOrder, Integer displayOrder,
            String cardDescription, Boolean active, Boolean assignable, String permissionCode) {
        WorkcenterDto dto = new WorkcenterDto(id, description, defaultFunction);
        dto.setRouteKey(routeKey); dto.setRoutePath(routePath); dto.setPath(routePath);
        dto.setIconKey(iconKey); dto.setGroupCode(groupCode); dto.setGroupTitle(groupTitle);
        dto.setGroupDescription(groupDescription); dto.setSectionCode(sectionCode);
        dto.setSectionTitle(sectionTitle); dto.setSectionDisplayOrder(sectionDisplayOrder);
        dto.setGroupDisplayOrder(groupDisplayOrder); dto.setDisplayOrder(displayOrder);
        dto.setCardDescription(cardDescription); dto.setActive(active); dto.setAssignable(assignable);
        dto.setPermissionCode(permissionCode);
        return dto;
    }

}
