package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.dto.v2.group.GroupSocietyRequest;
import za.co.mawa.bes.entity.PartnerEntity;
import za.co.mawa.bes.entity.ProductEntity;
import za.co.mawa.bes.entity.v2.GroupSocietyEntity;
import za.co.mawa.bes.repository.PartnerRepository;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyAccountTxnRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyContactRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyMemberRepository;
import za.co.mawa.bes.repository.v2.GroupSocietyRepository;
import za.co.mawa.bes.service.PartnerServiceV2;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupSocietyServiceTest {

    @Mock
    private GroupSocietyRepository groupSocietyRepository;
    @Mock
    private GroupSocietyContactRepository contactRepository;
    @Mock
    private GroupSocietyMemberRepository memberRepository;
    @Mock
    private GroupSocietyAccountTxnRepository accountTxnRepository;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PartnerServiceV2 partnerServiceV2;
    @Mock
    private NumberAllocationService numberAllocationService;
    @Mock
    private ReferenceDataValidationService referenceDataValidationService;

    private GroupSocietyService service;

    @BeforeEach
    void setUp() {
        service = new GroupSocietyService(
                groupSocietyRepository,
                contactRepository,
                memberRepository,
                accountTxnRepository,
                partnerRepository,
                productRepository,
                partnerServiceV2,
                numberAllocationService,
                referenceDataValidationService
        );
    }

    @Test
    void createAllocatesTheGroupNumberAndLinksTheSelectedProduct() throws Exception {
        GroupSocietyRequest request = new GroupSocietyRequest();
        set(request, "partnerId", "partner-1");
        set(request, "productId", "product-1");
        set(request, "societyType", "society");
        set(request, "groupNo", "CLIENT-SUPPLIED-NUMBER");
        set(request, "openingBalanceCents", 0L);

        ProductEntity product = ProductEntity.builder()
                .id("product-1")
                .code("GS-COVER-1")
                .description("GROUP SOCIETY COVER")
                .type("GROUP-SOCIETY")
                .build();
        PartnerEntity partner = PartnerEntity.builder()
                .id("partner-1")
                .no("P000001")
                .name1("TEST SOCIETY")
                .type("GROUP")
                .build();

        when(numberAllocationService.allocateNumber("GROUP_SOCIETY")).thenReturn("GS-000001");
        when(groupSocietyRepository.existsByGroupNo("GS-000001")).thenReturn(false);
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
        when(partnerRepository.findById("partner-1")).thenReturn(Optional.of(partner));
        when(groupSocietyRepository.existsByPartnerId("partner-1")).thenReturn(false);
        when(groupSocietyRepository.save(any(GroupSocietyEntity.class))).thenAnswer(invocation -> {
            GroupSocietyEntity saved = invocation.getArgument(0);
            saved.setId("society-1");
            return saved;
        });

        GroupSocietyEntity result = service.create(request);

        assertEquals("GS-000001", result.getGroupNo());
        assertEquals("product-1", result.getProductId());
        assertEquals("SOCIETY", result.getSocietyType());
        assertEquals("GS-COVER-1", result.getProductCode());
        verify(numberAllocationService).allocateNumber("GROUP_SOCIETY");
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
