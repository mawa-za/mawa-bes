package za.co.mawa.bes.entity.v2;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.enums.CasePartyType;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "case_party")
public class CasePartyEntity {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Column(name = "partner_id")
    private String partnerId;

    @Column(name = "party_name", nullable = false)
    private String partyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false)
    private CasePartyType partyType;

    @Column(name = "id_number")
    private String idNumber;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "attorney_firm")
    private String attorneyFirm;

    @Column(name = "attorney_name")
    private String attorneyName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
