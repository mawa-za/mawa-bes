package za.co.mawa.bes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the current dedicated-domain identifier and its legacy generic
 * transaction identifier so attachments created before a domain migration
 * remain visible from the new screens.
 */
@Service
public class LegacyAttachmentObjectIdResolver {

    private static final Logger log = LoggerFactory.getLogger(LegacyAttachmentObjectIdResolver.class);

    private static final List<LegacyIdMapping> MAPPINGS = List.of(
            new LegacyIdMapping("membership", "old_id"),
            new LegacyIdMapping("membership_plan", "old_id"),
            new LegacyIdMapping("membership_claim", "legacy_transaction_id"),
            new LegacyIdMapping("payment_request", "legacy_transaction_id"),
            new LegacyIdMapping("group_society", "legacy_transaction_id"),
            new LegacyIdMapping("group_society_account_txn", "legacy_transaction_id"),
            new LegacyIdMapping("cashup", "legacy_transaction_id"),
            new LegacyIdMapping("cashup_deposit", "legacy_transaction_id"),
            new LegacyIdMapping("cashup_receipt", "legacy_transaction_id")
    );

    private final JdbcTemplate jdbcTemplate;

    public LegacyAttachmentObjectIdResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns the requested id first, followed by any current/legacy aliases.
     * The inverse lookup is intentional: callers can pass either a new id or a
     * legacy id and still receive the same attachment set.
     */
    public List<String> resolveObjectIds(String objectId) {
        if (!StringUtils.hasText(objectId)) {
            return List.of();
        }

        String requestedId = objectId.trim();
        Set<String> resolvedIds = new LinkedHashSet<>();
        resolvedIds.add(requestedId);

        for (LegacyIdMapping mapping : MAPPINGS) {
            resolveMapping(mapping, requestedId, resolvedIds);
        }
        return new ArrayList<>(resolvedIds);
    }

    private void resolveMapping(LegacyIdMapping mapping, String objectId, Set<String> resolvedIds) {
        String sql = "SELECT id, " + mapping.legacyColumn()
                + " FROM " + mapping.tableName()
                + " WHERE id = ? OR " + mapping.legacyColumn() + " = ?";
        try {
            jdbcTemplate.query(sql, rs -> {
                addIfPresent(resolvedIds, rs.getString("id"));
                addIfPresent(resolvedIds, rs.getString(mapping.legacyColumn()));
            }, objectId, objectId);
        } catch (DataAccessException ex) {
            // Tenant schemas may be at different migration levels during a rollout.
            // Missing optional lineage columns must not prevent normal attachments
            // from loading by their current object id.
            log.debug("Could not resolve attachment lineage from {}.{}: {}",
                    mapping.tableName(), mapping.legacyColumn(), ex.getMostSpecificCause().getMessage());
        }
    }

    private void addIfPresent(Set<String> ids, String value) {
        if (StringUtils.hasText(value)) {
            ids.add(value.trim());
        }
    }

    private record LegacyIdMapping(String tableName, String legacyColumn) {
    }
}
