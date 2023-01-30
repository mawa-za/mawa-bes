package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;
import za.co.mawa.bes.dto.TransactionDto;

import java.io.Serializable;
import java.util.Date;

    /**
     *
     * @author tebogomohale
     */
    @Entity
    @Table(name = "transaction")
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @Getter
    @Setter
    public class TransactionEntity implements Serializable {
        private static final long serialVersionUID = 1L;
        @Id
        @Basic(optional = false)
        @Column(name = "id", length = 20)
        private String id;
        @Column(name = "type", length = 20)
        private String type;
        @Column(name = "sub_type", length = 45)
        private String subType;
        @Column(name = "description", length = 255)
        private String description;
        @Column(name = "valid_from")
        @Temporal(TemporalType.DATE)
        private Date validFrom;
        @Column(name = "valid_to")
        @Temporal(TemporalType.DATE)
        private Date validTo;
        @Column(name = "status", length = 45)
        private String status;
        @Column(name = "status_reason", length = 45)
        private String statusReason;
        @Column(name = "sub_status", length = 45)
        private String subStatus;
        @Column(name = "location", length = 100)
        private String location;
        @Lob
        @Column(name = "sub_description")
        private String subDescription;
        @Column(name = "createdBy", length = 45)
        private String createdBy;
        @Column(name = "changedBy", length = 45)
        private String changedBy;

        public TransactionEntity(TransactionDto transactionDto){
            this.id = transactionDto.getId();
            this.type = transactionDto.getType();
            this.subType = transactionDto.getSubType();
            this.description = transactionDto.getDescription();
            this.status = transactionDto.getStatus();
        }
    }


