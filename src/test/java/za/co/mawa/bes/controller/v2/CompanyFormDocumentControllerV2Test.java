package za.co.mawa.bes.controller.v2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.mawa.bes.service.UserAccessService;
import za.co.mawa.bes.service.v2.CompanyFormDocumentService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyFormDocumentControllerV2Test {

    @Mock
    private CompanyFormDocumentService service;
    @Mock
    private UserAccessService userAccessService;

    private CompanyFormDocumentControllerV2 controller;

    @BeforeEach
    void setUp() {
        controller = new CompanyFormDocumentControllerV2(service, userAccessService);
    }

    @Test
    void publishedFormsRemainVisibleToOrdinaryAuthenticatedUsers() {
        List<Map<String, Object>> forms = List.of(Map.of("id", "form-1", "active", true));
        when(service.list(true)).thenReturn(forms);

        var response = controller.list(true);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(forms, response.getBody());
        verify(userAccessService, never()).isProtectedAdministrator();
    }

    @Test
    void protectedAdministratorCanViewUnpublishedForms() {
        when(userAccessService.isProtectedAdministrator()).thenReturn(true);
        when(service.list(false)).thenReturn(List.of());

        var response = controller.list(false);

        assertEquals(200, response.getStatusCode().value());
        verify(service).list(false);
    }

    @Test
    void ordinaryUserCannotViewUnpublishedForms() {
        when(userAccessService.isProtectedAdministrator()).thenReturn(false);

        SecurityException error = assertThrows(SecurityException.class, () -> controller.list(false));

        assertEquals("Only a protected tenant administrator can view unpublished company forms", error.getMessage());
        verify(service, never()).list(false);
    }
}
