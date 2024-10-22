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

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "transaction_subtype")
    private String transactionSubtype;

    @Column(name = "transaction_number")
    private String transactionNumber;

    @Column(name = "identity_type")
    private String identityType;

    @Column(name = "identity_number")
    private String identityNumber;

    @Column(name = "main_partner")
    private String mainPartner;


    @Column(name = "employee_responsible")
    private String employeeResponsible;

    @Column(name = "product")
    private String product;

    @Column(name = "creation_date")
    private Date creationDate;

    @Column(name = "category")
    private String category;

    @Column(name = "priority")
    private String priority;

    @Column(name = "transaction_status")
    private String transactionStatus;


}

