package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "partner_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerResourceApiEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    //@NotNull
    @Column(name = "resource_id", length = 20)
    private String resource_id;

    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner_url", length = 20)
    private String partner_url;

    @Basic(optional = false)
    //@NotNull
    @Column(name = "partner_no", length = 45)
    private String partner_no;

    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;

    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "status_reason", length = 20)
    private String status_reason;

    @Basic(optional = false)
    //@NotNull
    @Column(name = "port_number", length = 20)
    private String port_number;

    @Column(name = "resource_name", length = 45)
    private String resource_name;

}
