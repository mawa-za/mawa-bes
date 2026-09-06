package za.co.mawa.bes.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import za.co.mawa.bes.dto.product.ProductDto;

import java.io.Serializable;
import java.util.Date;
@Entity
@Table(name = "product")
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class ProductEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private String id;
    @Column(unique = true, name = "code")
    private String code;
    @Column(name = "description")
    private String description;
    @Column(name = "type")
    private String type;
    @Column(name = "uom")
    private String uom;
    @Column(name = "category_id")
    private String categoryId;
    @Builder.Default
    @Column(name = "available_for_sale", nullable = false)
    private Boolean availableForSale = true;
    @Column(name = "xero_item_id", length = 64)
    private String xeroItemId;
    @Column(name = "valid_from")
    @Temporal(TemporalType.DATE)
    private Date validFrom;
    @Column(name = "valid_to")
    @Temporal(TemporalType.DATE)
    private Date validTo;

}
