package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "funeral_membership_cover")
public class FuneralMembershipCoverEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "identity_number", nullable = false)
    private String identityNumber;

    @Column(name = "membership_id", nullable = false)
    private String membershipId;

    @Column(name = "membership_number")
    private String membershipNumber;

    @Column(name = "burial_society_partner_id")
    private String burialSocietyPartnerId;

    @Column(name = "burial_society_name", nullable = false)
    private String burialSocietyName;

    @Column(name = "cover_amount_cents", nullable = false)
    private Long coverAmountCents = 0L;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";
}
