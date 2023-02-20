package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

import java.util.Date;

@Entity
@Table(name = "number_range")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class NumberRangeEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(unique = true,name = "object", length = 20)
    private String object;
    @Column(name = "prefix", length = 20)
    private String prefix;
    @Column(name = "start", length = 40)
    private String start;
    @Column(name = "current", length = 40)
    private String current;
    @Column(name = "end", length = 40)
    private String end;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

}
