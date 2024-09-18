package za.co.mawa.bes.entity.transaction;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.Date;

public class TransactionView {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(name = "number")
    private String number;
    @Column(name = "type")
    private String type;
    @Column(name = "sub_type")
    private String subType;
    @Column(name = "description")
    private String description;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;
    @Column(name = "status")
    private String status;
    @Column(name = "status_reason")
    private String statusReason;
    @Column(name = "sub_status")
    private String subStatus;
    @Column(name = "location")
    private String location;
    @Column(name = "category")
    private String category;
    @Lob
    @Column(name = "sub_description")
    private String subDescription;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "changedBy")
    private String changedBy;
    @Column(name = "priority")
    private String priority;
}
