package za.co.mawa.bes.entity.transaction;

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
@Table(name = "transaction_view")
public class TransactionViewEntity implements Serializable {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "transaction_number")
    private String transactionNumber;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "creation_date")
    private Date creationDate;

    @Column(name = "transaction_type_desc")
    private String transactionTypeDesc;

    @Column(name = "transaction_status")
    private String transactionStatus;

    @Column(name = "customer_identity_number")
    private String customerIdentityNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "employee_responsible")
    private String employeeResponsible;

    @Column(name = "filter")
    private String filter;

}

