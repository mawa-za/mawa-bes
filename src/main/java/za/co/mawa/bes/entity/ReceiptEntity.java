package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "receipt")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class ReceiptEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "receipt_number" , length = 45)
    private String receiptNumber;
    @Column(name = "ext_receipt_number",length = 45)
    private String extReceiptNumber;
    @Column(name = "receipt_type",length = 45)
    private String receiptType;
    @Column(name = "creation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;
    @Column(name = "creation_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;
    @Column(name = "created_by",length = 45)
    private String createdBy;
    @Column(name = "transaction_id",length = 45)
    private String transaction;
    @Column(name = "invoice_number",length = 45)
    private String invoiceNumber;
    @Column(name = "membership_number",length = 45)
    private String membershipNumber;
    @Column(name = "membership_period",length = 45)
    private String membershipPeriod;
    @Column(name = "tender_type",length = 45)
    private String tenderType;
    @Column(name = "location")
    private String location;
    @Column(name = "amount")
    private BigDecimal amount;
}
