package za.co.mawa.bes.dto.v2;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FuneralPackageItemRequestDto { private String productId; private Integer quantity; private Long unitPriceCents; }
