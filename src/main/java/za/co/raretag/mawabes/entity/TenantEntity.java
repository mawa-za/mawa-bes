package za.co.raretag.mawabes.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tenant")
public class TenantEntity  implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 45)
    private String id;
    @Column(name = "name", length = 45)
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
