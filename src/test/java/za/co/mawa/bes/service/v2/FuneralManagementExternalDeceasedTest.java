package za.co.mawa.bes.service.v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import za.co.mawa.bes.entity.v2.FuneralServiceEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuneralManagementExternalDeceasedTest {
    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks FuneralManagementService service;

    @Test
    void externalDependentIdentityResolvesToDependentRatherThanMainMembershipHolder() {
        FuneralServiceEntity funeral = funeralWithIdentity("8101011234089");
        var dependent = new FuneralManagementService.ExternalDeceasedReference("deceased-dependent", "DEPENDENT");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class),
                eq("membership-1"), eq("membership-1"), eq("8101011234089")))
                .thenReturn(List.of(dependent));

        var actual = service.resolveExternalDeceasedForFuneral("other-tenant", "membership-1", funeral);

        assertEquals("deceased-dependent", actual.partnerId());
        assertEquals("DEPENDENT", actual.deceasedType());
        verify(jdbcTemplate).query(
                org.mockito.ArgumentMatchers.argThat((String sql) ->
                        sql.contains("`other-tenant`.`membership_dependent`")
                                && sql.contains("md.active = 1")
                                && sql.contains("matched.partner_id")
                                && sql.contains("`other-tenant`.`partner_identity`")),
                any(RowMapper.class), eq("membership-1"), eq("membership-1"), eq("8101011234089"));
    }

    @Test
    void externalMainMemberIdentityStillResolvesCorrectly() {
        var holder = new FuneralManagementService.ExternalDeceasedReference("main-member", "MAIN_MEMBER");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class),
                eq("membership-1"), eq("membership-1"), eq("7302201234089")))
                .thenReturn(List.of(holder));
        var actual = service.resolveExternalDeceasedForFuneral(
                "other-tenant", "membership-1", funeralWithIdentity("7302201234089"));
        assertEquals(holder, actual);
    }

    @Test
    void ambiguousExternalIdentityMustNotBeAssignedToTheMembershipHolder() {
        var funeral = funeralWithIdentity("8101011234089");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class),
                eq("membership-1"), eq("membership-1"), eq("8101011234089")))
                .thenReturn(List.of(
                        new FuneralManagementService.ExternalDeceasedReference("holder", "MAIN_MEMBER"),
                        new FuneralManagementService.ExternalDeceasedReference("dependent", "DEPENDENT")));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.resolveExternalDeceasedForFuneral("other-tenant", "membership-1", funeral));
        assertTrue(exception.getMessage().contains("uniquely matched"));
    }

    @Test
    void missingMatchingExternalPartnerMustStopClaimCreation() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class),
                eq("membership-1"), eq("membership-1"), eq("8101011234089")))
                .thenReturn(List.of());
        assertThrows(IllegalArgumentException.class,
                () -> service.resolveExternalDeceasedForFuneral(
                        "other-tenant", "membership-1", funeralWithIdentity("8101011234089")));
    }

    @Test
    void adultDeceasedWithoutIdentityCannotFallBackToMembershipHolder() {
        FuneralServiceEntity funeral = new FuneralServiceEntity();
        assertThrows(IllegalArgumentException.class,
                () -> service.resolveExternalDeceasedForFuneral("other-tenant", "membership-1", funeral));
    }

    private static FuneralServiceEntity funeralWithIdentity(String identity) {
        FuneralServiceEntity service = new FuneralServiceEntity();
        service.setDeceasedIdentityNumber(identity);
        return service;
    }
}
