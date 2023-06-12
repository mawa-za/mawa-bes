package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "address")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class AddressEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    //@Size(max = 45)
    @Column(name = "address_line_1", length = 45)
    private String addressLine1;
    //@Size(max = 45)
    @Column(name = "address_line_2", length = 45)
    private String addressLine2;
    //@Size(max = 45)
    @Column(name = "address_line_3", length = 45)
    private String addressLine3;
    // @Size(max = 45)
    @Column(name = "address_line_4", length = 45)
    private String addressLine4;
    // @Size(max = 5)
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

}
