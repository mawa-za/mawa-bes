package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Date;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@Entity
@Table(name = "partner_bank_account")
public class PartnerBankingDetailsEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "partner")
    private String partner;
    @Column(name = "account_holder")
    private String accountHolder;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "account_type")
    private String accountType;
    @Column(name = "bank_name")
    private String bankName;
    @Column(name = "branch_code")
    private String branchCode;
    @Column(name = "branch_name")
    private String branchName;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Column(name = "status")
    private String status;

}
