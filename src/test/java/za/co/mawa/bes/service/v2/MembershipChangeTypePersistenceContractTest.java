package za.co.mawa.bes.service.v2;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;
import za.co.mawa.bes.entity.v2.MembershipChangeRequestEntity;
import za.co.mawa.bes.enums.MembershipChangeType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for VARCHAR(30) truncation during dependent claim eligibility requests.
 * The database migration must keep the same capacity as this entity mapping.
 */
class MembershipChangeTypePersistenceContractTest {

    @Test
    void allMembershipChangeTypeEnumValuesFitColumnMapping() throws Exception {
        Column column = MembershipChangeRequestEntity.class
                .getDeclaredField("changeType").getAnnotation(Column.class);
        assertNotNull(column);
        for (MembershipChangeType changeType : MembershipChangeType.values()) {
            assertTrue(changeType.name().length() <= column.length(),
                    () -> changeType.name() + " (" + changeType.name().length()
                            + " characters) exceeds the change_type column length " + column.length());
        }
    }
}
