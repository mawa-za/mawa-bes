package za.co.mawa.bes.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductTypeCodeTest {

    @Test
    void groupSocietyIsSelectableAsASpecialisedNonStockProduct() {
        ProductTypeCode type = ProductTypeCode.requireSelectable("group_society");

        assertEquals(ProductTypeCode.GROUP_SOCIETY, type);
        assertEquals("GROUP-SOCIETY", type.getCode());
        assertFalse(type.isStockControlled());
        assertFalse(type.isBundle());
        assertTrue(type.isSpecialisedWorkflow());
    }
}
