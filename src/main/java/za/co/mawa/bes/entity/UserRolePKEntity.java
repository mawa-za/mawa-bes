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
public class UserRolePKEntity implements Serializable {

    @Column(name = "user")
    private String user;
    @Column(name = "role")
    private String role;

}

