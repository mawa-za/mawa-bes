package za.co.mawa.bes.controller.v2;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import za.co.mawa.bes.dto.partner.PartnerEditDto;
import za.co.mawa.bes.service.PartnerService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PartnerControllerV2Test {

    @Test
    void editPartnerBindsResourceIdFromPathBeforeSaving() {
        PartnerService partnerService = mock(PartnerService.class);
        PartnerControllerV2 controller = new PartnerControllerV2();
        controller.partnerService = partnerService;

        PartnerEditDto request = new PartnerEditDto();
        request.setName1("Updated supplier");

        ResponseEntity<?> response = controller.editPartner("partner-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("partner-1", request.getId());
        verify(partnerService).edit(request);
    }
}
