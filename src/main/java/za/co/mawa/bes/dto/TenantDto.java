package za.co.mawa.bes.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
public class TenantDto implements Serializable {
    private String id;
    private String name;
    private String url;
    private String status;
    private String database_url;
    private String database_username;
    private String database_password;

}
