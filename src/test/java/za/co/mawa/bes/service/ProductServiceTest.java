package za.co.mawa.bes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.product.ProductCreateDto;
import za.co.mawa.bes.dto.product.pricing.ProductPricingCreateDto;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.entity.ProductPricingEntity;
import za.co.mawa.bes.entity.ProductPricingPKEntity;
import za.co.mawa.bes.entity.product.ProductCategoryMasterEntity;
import za.co.mawa.bes.exception.ProductCreationFailure;
import za.co.mawa.bes.repository.ProductPricingRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.service.v2.ProductClassificationService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductClassificationService productClassificationService;
    @Mock
    private ProductPricingRepository productPricingRepository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService();
        service.productRepository = productRepository;
        service.productClassificationService = productClassificationService;
        service.productPricingRepository = productPricingRepository;
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

    @Test
    void addPricingNormalisesLegacyUnderscoreCodeAndUpdatesExistingPrice() throws Exception {
        ProductPricingPKEntity key = ProductPricingPKEntity.builder()
                .product("product-1")
                .pricing("SELLING-PRICE")
                .build();
        ProductPricingEntity existing = ProductPricingEntity.builder()
                .productPricingPKEntity(key)
                .value(new BigDecimal("100.00"))
                .build();
        when(productPricingRepository.findById(key)).thenReturn(Optional.of(existing));

        ProductPricingCreateDto request = new ProductPricingCreateDto();
        request.setProduct("product-1");
        request.setPricing("SELLING_PRICE");
        request.setValue(new BigDecimal("125.50"));

        service.addPricing(request);

        assertEquals("SELLING-PRICE", existing.getProductPricingPKEntity().getPricing());
        assertEquals(new BigDecimal("125.50"), existing.getValue());
        verify(productPricingRepository).save(existing);
    }

    @Test
    void addPricingCreatesSellingPriceWhenPricingTypeIsMissing() throws Exception {
        ProductPricingCreateDto request = new ProductPricingCreateDto();
        request.setProduct("product-2");
        request.setValue(new BigDecimal("80.00"));
        when(productPricingRepository.findById(any(ProductPricingPKEntity.class)))
                .thenReturn(Optional.empty());

        service.addPricing(request);

        verify(productPricingRepository).save(org.mockito.ArgumentMatchers.argThat(entity ->
                "product-2".equals(entity.getProductPricingPKEntity().getProduct())
                        && "SELLING-PRICE".equals(entity.getProductPricingPKEntity().getPricing())
                        && new BigDecimal("80.00").equals(entity.getValue())));
    }
}
