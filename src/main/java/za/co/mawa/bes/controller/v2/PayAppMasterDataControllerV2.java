package za.co.mawa.bes.controller.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import za.co.mawa.bes.dto.v2.payapp.PayAppMemberSyncDto;
import za.co.mawa.bes.dto.v2.payapp.PayAppPageResponse;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("v2/pay-app")
public class PayAppMasterDataControllerV2 {
    private static final int MAX_PAGE_SIZE = 2000;
    private final JdbcTemplate jdbcTemplate;

    public PayAppMasterDataControllerV2(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/members")
    public ResponseEntity<PayAppPageResponse<PayAppMemberSyncDto>> members(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM membership", Long.class);
        long total = count == null ? 0L : count;

        String sql = """
            SELECT m.id membership_id, m.membership_no, m.member_id partner_id,
                   p.number partner_no, p.name1, p.name2, p.name3,
                   p.status partner_status, p.birth_date, p.gender,
                   (SELECT UPPER(TRIM(pi.type)) FROM partner_identity pi
                     WHERE pi.partner = p.id
                       AND UPPER(TRIM(pi.type)) IN ('SA-ID','PASSPORT')
                       AND NULLIF(TRIM(pi.value),'') IS NOT NULL
                     ORDER BY CASE WHEN UPPER(TRIM(pi.type))='SA-ID' THEN 0 ELSE 1 END,pi.type,pi.value LIMIT 1) identity_type,
                   (SELECT TRIM(pi.value) FROM partner_identity pi
                     WHERE pi.partner = p.id
                       AND UPPER(TRIM(pi.type)) IN ('SA-ID','PASSPORT')
                       AND NULLIF(TRIM(pi.value),'') IS NOT NULL
                     ORDER BY CASE WHEN UPPER(TRIM(pi.type))='SA-ID' THEN 0 ELSE 1 END,pi.type,pi.value LIMIT 1) identity_number,
                   m.plan_id, m.status membership_status,
                   COALESCE(
                       NULLIF(m.paid_up_to_period, ''),
                       (SELECT MAX(mp.period_yyyymm)
                          FROM membership_premium mp
                         WHERE (mp.membership_id = m.id OR mp.membership_id = m.old_id)
                           AND mp.status = 'PAID')
                   ) paid_up_to_period,
                   m.join_date, COALESCE(m.updated_at, m.created_at) updated_at
              FROM membership m
              JOIN partner p ON p.id = m.member_id
             ORDER BY m.id
             LIMIT ? OFFSET ?
            """;

        List<PayAppMemberSyncDto> content = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date birthDate = rs.getDate("birth_date");
            Date joinDate = rs.getDate("join_date");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            return PayAppMemberSyncDto.builder()
                    .membershipId(rs.getString("membership_id"))
                    .membershipNo(rs.getString("membership_no"))
                    .partnerId(rs.getString("partner_id"))
                    .partnerNo(rs.getString("partner_no"))
                    .firstName(rs.getString("name1"))
                    .lastName(rs.getString("name2"))
                    .middleName(rs.getString("name3"))
                    .identityType(rs.getString("identity_type"))
                    .identityNumber(rs.getString("identity_number"))
                    .partnerStatus(rs.getString("partner_status"))
                    .birthDate(birthDate == null ? null : birthDate.toLocalDate())
                    .gender(rs.getString("gender"))
                    .planId(rs.getString("plan_id"))
                    .membershipStatus(rs.getString("membership_status"))
                    .paidUpToPeriod(rs.getString("paid_up_to_period"))
                    .joinDate(joinDate == null ? null : joinDate.toLocalDate())
                    .updatedAt(updatedAt == null ? LocalDateTime.now() : updatedAt.toLocalDateTime())
                    .build();
        }, safeSize, offset);

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return ResponseEntity.ok(new PayAppPageResponse<>(content, safePage, safeSize, total,
                totalPages, safePage + 1 >= totalPages));
    }
}
