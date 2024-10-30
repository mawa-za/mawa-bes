package za.co.mawa.bes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "partner_view")
public class PartnerViewEntity implements Serializable {
    @Id
    @Column(name = "partner_id")
    private String partnerId;

    @Column(name = "partner_no")
    private String partnerNo;

    @Column(name = "partner_type")
    private String partnerType;

    @Column(name = "partner_role")
    private String partnerRole;

    @Column(name = "identity_type")
    private String identityType;

    @Column(name = "identity_number")
    private String identityNumber;

    @Column(name = "name1")
    private String name1;

    @Column(name = "name2")
    private String name2;

    @Column(name = "name3")
    private String name3;

    @Column(name = "birth_date")
    private Date birthDate;

    @Column(name = "gender")
    private String gender;

    @Column(name = "title")
    private String title;

    @Column(name = "status")
    private String status;

    @Column(name = "marital_status")
    private String maritalStatus;

}