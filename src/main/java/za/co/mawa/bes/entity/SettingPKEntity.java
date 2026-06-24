package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;


/**
 *
 * @author tebogomohale
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingPKEntity implements Serializable {

    @Basic(optional = false)
    @Column(name = "setting", length = 20)
    private String setting;
    @Basic(optional = false)
    @Column(name = "attribute", length = 60)
    private String attribute;

}

