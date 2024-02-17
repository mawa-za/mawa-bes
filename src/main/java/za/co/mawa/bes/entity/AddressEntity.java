package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

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
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "type")
    private String type;
    @Column(name = "object_id")
    private String objectId;
    @Column(name = "address_line_1")
    private String addressLine1;
    //@Size(max = 45)
    @Column(name = "address_line_2")
    private String addressLine2;
    @Column(name = "address_line_3", length = 45)
    private String addressLine3;
    @Column(name = "address_line_4", length = 45)
    private String addressLine4;
    @Column(name = "suburb", length = 45)
    private String suburb;
    @Column(name = "town", length = 45)
    private String town;
    @Column(name = "city", length = 45)
    private String city;
    @Column(name = "province", length = 45)
    private String province;
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

}
