package za.co.mawa.bes.service.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.repository.v2.MembershipRepository;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MembershipActionGuardService {
    private final MembershipRepository membershipRepository;
    private final JdbcTemplate jdbcTemplate;

    public void requireActionable(String membershipId) {
        if (membershipId == null || membershipId.isBlank()) return;
        MembershipEntity membership = membershipRepository.findById(membershipId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        requireActionable(membership);
    }

    public void requireActionableForObject(String objectId) {
        if (objectId == null || objectId.isBlank()) return;
        String id = objectId.trim();

        MembershipEntity membership = membershipRepository.findById(id).orElse(null);
        if (membership != null) {
            requireActionable(membership);
            return;
        }

        List<String> claimMembershipIds = jdbcTemplate.query(
                "SELECT membership_id FROM membership_claim WHERE id=? LIMIT 1",
                (rs, rowNum) -> rs.getString("membership_id"),
                id);
        if (!claimMembershipIds.isEmpty()) {
            requireActionable(claimMembershipIds.get(0));
            return;
        }

        List<String> paymentRequestMembershipIds = jdbcTemplate.query(
                """
                SELECT mc.membership_id
                  FROM payment_request pr
                  JOIN membership_claim mc ON mc.id = pr.source_id
                 WHERE pr.id=?
                   AND pr.source_type='MEMBERSHIP_CLAIM'
                 LIMIT 1
                """,
                (rs, rowNum) -> rs.getString("membership_id"),
                id);
        if (!paymentRequestMembershipIds.isEmpty()) {
            requireActionable(paymentRequestMembershipIds.get(0));
        }
    }

    public void requireActionable(MembershipEntity membership) {
        if (membership == null) return;
        if (membership.getStatus() != null && "MERGED".equalsIgnoreCase(membership.getStatus().trim())) {
            throw new IllegalStateException("This membership was merged into " +
                    (membership.getMergedIntoMembershipId() == null ? "another membership" : membership.getMergedIntoMembershipId()) +
                    " and is read-only.");
        }
        if (isLapsed(membership.getStatus())) {
            throw new IllegalStateException(
                    "This membership is LAPSED. Reactivate the membership and wait for approval before performing any other action.");
        }
    }

    public boolean isLapsed(String status) {
        return status != null && "LAPSED".equals(status.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
