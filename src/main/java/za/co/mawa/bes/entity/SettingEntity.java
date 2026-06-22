package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;


/**
 *
 * @author tebogomohale
 */
@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected SettingPKEntity settingsPK;
    @Column(name = "value", length = 60)
    private String value;

}
