package za.co.mawa.bes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.entity.product.ProductCategoryMasterEntity;
import za.co.mawa.bes.exception.ProductCreationFailure;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.service.v2.ProductClassificationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductClassificationService productClassificationService;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService();
        service.productRepository = productRepository;
        service.productClassificationService = productClassificationService;
    }

    @Test
    void createRejectsDuplicateCodeBeforeWritingAnotherProduct() {
        ProductCreateDto request = new ProductCreateDto();
        request.setCode(" gs-cover ");
        request.setDescription("Group society cover");
        request.setType("GROUP-SOCIETY");
        request.setCategoryId("CAT-GROUP-SOCIETY");

        ProductCategoryMasterEntity category = ProductCategoryMasterEntity.builder()
                .id("CAT-GROUP-SOCIETY")
                .code("GROUP-SOCIETY")
                .name("Group Society")
                .productType("GROUP-SOCIETY")
                .active(true)
                .build();
        when(productClassificationService.requireActiveCategory(
                "CAT-GROUP-SOCIETY", "GROUP-SOCIETY"))
                .thenReturn(category);
        when(productRepository.findByCode("GS-COVER"))
                .thenReturn(ProductEntity.builder().id("existing-product").code("GS-COVER").build());

        ProductCreationFailure exception = assertThrows(
                ProductCreationFailure.class,
                () -> service.create(request)
        );

        assertEquals("A product with code GS-COVER already exists.", exception.getMessage());
        verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any(ProductEntity.class));
    }
}
