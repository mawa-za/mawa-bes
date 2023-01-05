package za.co.raretag.mawabes.dto;

public class WorkcenterDto {
    private String id;
    private String description;
    private String defaultFunction;

    public WorkcenterDto() {
    }

    public WorkcenterDto(String id, String description, String defaultFunction) {
        this.id = id;
        this.description = description;
        this.defaultFunction = defaultFunction;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultFunction() {
        return defaultFunction;
    }

    public void setDefaultFunction(String defaultFunction) {
        this.defaultFunction = defaultFunction;
    }
}
