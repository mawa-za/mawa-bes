package za.co.mawa.bes.service.v2;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class MembershipWaitingPeriodService {
    private final JdbcTemplate jdbcTemplate;

    public MembershipWaitingPeriodService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${mawa.membership.waiting-period-cron:0 5 0 * * *}")
    @Transactional
    public void activateEligibleMemberships() {
        jdbcTemplate.update("""
                UPDATE membership
                   SET status = 'ACTIVE', updated_at = UTC_TIMESTAMP(), updated_by = 'SYSTEM'
                 WHERE UPPER(REPLACE(status, '-', '_')) = 'WAITING_PERIOD'
                   AND benefit_eligible_from IS NOT NULL
                   AND benefit_eligible_from <= ?
                   AND NOT EXISTS (
                       SELECT 1 FROM membership_status_change_request pending
                        WHERE pending.membership_id = membership.id
                          AND pending.status = 'PENDING_APPROVAL'
                   )
                """, LocalDate.now());
    }
}
