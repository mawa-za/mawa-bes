package za.co.mawa.bes.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.configuration.context.UserContext;
import za.co.mawa.bes.dto.v2.*;
import za.co.mawa.bes.entity.NumberRangeEntity;
import za.co.mawa.bes.entity.v2.NumberRangeAllocationEntity;
import za.co.mawa.bes.entity.v2.NumberSequenceEntity;
import za.co.mawa.bes.mapper.v2.NumberRangeAllocationMapper;
import za.co.mawa.bes.mapper.v2.NumberSequenceMapper;
import za.co.mawa.bes.repository.NumberRangeRepository;
import za.co.mawa.bes.repository.v2.NumberRangeAllocationRepository;
import za.co.mawa.bes.repository.v2.NumberSequenceRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NumberRangeConfigurationService {
    private static final long DEFAULT_END_NO = 9_999_999_999L;
    private static final int DEFAULT_ALLOCATION_SIZE = 1000;
    private static final long DEFAULT_WARNING_THRESHOLD = 1000L;
    private static final Pattern RANGE_KEY_PATTERN = Pattern.compile("^[A-Z0-9_-]{2,64}$");
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)$");

    private final NumberSequenceRepository numberSequenceRepository;
    private final NumberRangeAllocationRepository allocationRepository;
    private final NumberRangeRepository legacyRepository;
    private final NumberSequenceMapper sequenceMapper;
    private final NumberRangeAllocationMapper allocationMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public NumberRangeConfigurationService(
            NumberSequenceRepository numberSequenceRepository,
            NumberRangeAllocationRepository allocationRepository,
            NumberRangeRepository legacyRepository,
            NumberSequenceMapper sequenceMapper,
            NumberRangeAllocationMapper allocationMapper,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.numberSequenceRepository = numberSequenceRepository;
        this.allocationRepository = allocationRepository;
        this.legacyRepository = legacyRepository;
        this.sequenceMapper = sequenceMapper;
        this.allocationMapper = allocationMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<NumberSequenceResponseDto> listSequences(String query, Boolean active) {
        String normalizedQuery = text(query) ? query.trim().toUpperCase(Locale.ROOT) : null;
        return numberSequenceRepository.findAllByOrderBySeqTypeAsc().stream()
                .filter(entity -> active == null || active.equals(entity.getActive()))
                .filter(entity -> normalizedQuery == null
                        || entity.getSeqType().contains(normalizedQuery)
                        || safe(entity.getDescription()).toUpperCase(Locale.ROOT).contains(normalizedQuery))
                .map(sequenceMapper::toResponse)
                .toList();
    }

    public NumberSequenceResponseDto getSequence(Long id) {
        return sequenceMapper.toResponse(requireSequence(id));
    }

    @Transactional
    public NumberSequenceResponseDto createSequence(NumberSequenceCreateRequestDto request) {
        if (request == null) throw new IllegalArgumentException("Number sequence request is required");
        String seqType = normalizeKey(request.getSeqType(), 64, "Sequence type");
        if (numberSequenceRepository.existsBySeqType(seqType)) {
            throw new IllegalArgumentException("Number sequence already exists for type: " + seqType);
        }

        long startNo = valueOr(request.getStartNo(), 1L);
        long nextNo = valueOr(request.getNextNo(), startNo);
        long endNo = valueOr(request.getEndNo(), DEFAULT_END_NO);
        int allocationSize = valueOr(request.getDefaultAllocationSize(), DEFAULT_ALLOCATION_SIZE);
        long warningThreshold = valueOr(request.getWarningThreshold(), DEFAULT_WARNING_THRESHOLD);
        boolean active = request.getActive() == null || request.getActive();
        String prefix = normalizePrefix(request.getPrefix());
        String separator = normalizeSeparator(request.getSeparator());
        int paddingLength = request.getPaddingLength() == null ? 0 : request.getPaddingLength();
        validateSequenceValues(startNo, nextNo, endNo, allocationSize, warningThreshold, null);
        validateFormatting(prefix, separator, paddingLength);

        NumberSequenceEntity entity = NumberSequenceEntity.builder()
                .seqType(seqType)
                .description(normalizeDescription(request.getDescription(), seqType))
                .prefix(prefix)
                .separator(separator)
                .paddingLength(paddingLength)
                .startNo(startNo)
                .nextNo(nextNo)
                .endNo(endNo)
                .defaultAllocationSize(allocationSize)
                .warningThreshold(warningThreshold)
                .active(active)
                .createdBy(actor())
                .updatedBy(actor())
                .build();
        try {
            NumberSequenceEntity saved = numberSequenceRepository.saveAndFlush(entity);
            NumberSequenceResponseDto response = sequenceMapper.toResponse(saved);
            audit("SEQUENCE", saved.getId().toString(), saved.getSeqType(), "CREATE", null, response);
            return response;
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Number sequence type must be unique", ex);
        }
    }

    @Transactional
    public NumberSequenceResponseDto updateSequence(Long id, NumberSequenceUpdateRequestDto request) {
        if (request == null) throw new IllegalArgumentException("Number sequence request is required");
        NumberSequenceEntity entity = requireSequence(id);
        NumberSequenceResponseDto before = sequenceMapper.toResponse(entity);

        if (text(request.getSeqType()) && !entity.getSeqType().equals(normalizeKey(request.getSeqType(), 64, "Sequence type"))) {
            throw new IllegalArgumentException("Sequence type cannot be changed after creation");
        }
        if (request.getLockVersion() != null && !request.getLockVersion().equals(entity.getLockVersion())) {
            throw new OptimisticLockException("This number sequence was changed by another user. Reload and try again.");
        }

        long startNo = valueOr(request.getStartNo(), entity.getStartNo());
        long nextNo = valueOr(request.getNextNo(), entity.getNextNo());
        long endNo = valueOr(request.getEndNo(), entity.getEndNo());
        int allocationSize = valueOr(request.getDefaultAllocationSize(), entity.getDefaultAllocationSize());
        long warningThreshold = valueOr(request.getWarningThreshold(), entity.getWarningThreshold());
        boolean active = request.getActive() == null ? entity.getActive() : request.getActive();
        String prefix = request.getPrefix() == null ? entity.getPrefix() : normalizePrefix(request.getPrefix());
        String separator = request.getSeparator() == null ? entity.getSeparator() : normalizeSeparator(request.getSeparator());
        int paddingLength = request.getPaddingLength() == null ? entity.getPaddingLength() : request.getPaddingLength();
        validateSequenceValues(startNo, nextNo, endNo, allocationSize, warningThreshold, entity);
        validateFormatting(prefix, separator, paddingLength);

        entity.setDescription(normalizeDescription(request.getDescription(), entity.getSeqType(), entity.getDescription()));
        entity.setPrefix(prefix);
        entity.setSeparator(separator);
        entity.setPaddingLength(paddingLength);
        entity.setStartNo(startNo);
        entity.setNextNo(nextNo);
        entity.setEndNo(endNo);
        entity.setDefaultAllocationSize(allocationSize);
        entity.setWarningThreshold(warningThreshold);
        entity.setActive(active);
        entity.setUpdatedBy(actor());

        NumberSequenceEntity saved = numberSequenceRepository.saveAndFlush(entity);
        NumberSequenceResponseDto response = sequenceMapper.toResponse(saved);
        audit("SEQUENCE", saved.getId().toString(), saved.getSeqType(), "UPDATE", before, response);
        return response;
    }

    public List<NumberRangeAllocationResponseDto> listAllocations(String seqType, String deviceId) {
        String normalizedType = text(seqType) ? normalizeKey(seqType, 64, "Sequence type") : null;
        String normalizedDevice = text(deviceId) ? deviceId.trim() : null;
        List<NumberRangeAllocationEntity> allocations;
        if (normalizedType != null && normalizedDevice != null) {
            allocations = allocationRepository.findTop200BySeqTypeAndDeviceIdOrderByIdDesc(normalizedType, normalizedDevice);
        } else if (normalizedType != null) {
            allocations = allocationRepository.findTop200BySeqTypeOrderByIdDesc(normalizedType);
        } else if (normalizedDevice != null) {
            allocations = allocationRepository.findTop200ByDeviceIdOrderByIdDesc(normalizedDevice);
        } else {
            allocations = allocationRepository.findTop200ByOrderByIdDesc();
        }
        return allocations.stream().map(allocationMapper::toResponse).toList();
    }

    public List<LegacyNumberRangeConfigurationResponseDto> listLegacyRanges(String query) {
        String normalizedQuery = text(query) ? query.trim().toUpperCase(Locale.ROOT) : null;
        return legacyRepository.findAllByOrderByObjectAsc().stream()
                .filter(entity -> normalizedQuery == null
                        || safe(entity.getObject()).toUpperCase(Locale.ROOT).contains(normalizedQuery)
                        || safe(entity.getPrefix()).toUpperCase(Locale.ROOT).contains(normalizedQuery))
                .map(this::toLegacyResponse)
                .toList();
    }

    @Transactional
    public LegacyNumberRangeConfigurationResponseDto createLegacyRange(LegacyNumberRangeConfigurationRequestDto request) {
        if (request == null) throw new IllegalArgumentException("Document number range request is required");
        String object = normalizeKey(request.getObject(), 20, "Object");
        if (legacyRepository.existsByObject(object)) {
            throw new IllegalArgumentException("Document number range already exists for object: " + object);
        }
        NumberRangeEntity entity = new NumberRangeEntity();
        entity.setObject(object);
        applyLegacyValues(entity, request, null);
        NumberRangeEntity saved = legacyRepository.saveAndFlush(entity);
        LegacyNumberRangeConfigurationResponseDto response = toLegacyResponse(saved);
        audit("LEGACY_RANGE", saved.getId().toString(), saved.getObject(), "CREATE", null, response);
        return response;
    }

    @Transactional
    public LegacyNumberRangeConfigurationResponseDto updateLegacyRange(Integer id, LegacyNumberRangeConfigurationRequestDto request) {
        if (request == null) throw new IllegalArgumentException("Document number range request is required");
        NumberRangeEntity entity = legacyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document number range not found: " + id));
        LegacyNumberRangeConfigurationResponseDto before = toLegacyResponse(entity);
        if (text(request.getObject()) && !entity.getObject().equals(normalizeKey(request.getObject(), 20, "Object"))) {
            throw new IllegalArgumentException("Object cannot be changed after creation");
        }
        applyLegacyValues(entity, request, before);
        NumberRangeEntity saved = legacyRepository.saveAndFlush(entity);
        LegacyNumberRangeConfigurationResponseDto response = toLegacyResponse(saved);
        audit("LEGACY_RANGE", saved.getId().toString(), saved.getObject(), "UPDATE", before, response);
        return response;
    }

    public List<Map<String, Object>> listAudit(String sourceType, String rangeKey) {
        StringBuilder sql = new StringBuilder("SELECT id,source_type,configuration_id,range_key,action,previous_value,new_value,changed_at,changed_by FROM number_range_configuration_audit WHERE 1=1");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (text(sourceType)) { sql.append(" AND source_type=?"); args.add(sourceType.trim().toUpperCase(Locale.ROOT)); }
        if (text(rangeKey)) { sql.append(" AND range_key=?"); args.add(rangeKey.trim().toUpperCase(Locale.ROOT)); }
        sql.append(" ORDER BY changed_at DESC,id DESC LIMIT 200");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        for (Map<String, Object> row : rows) {
            Object actor = row.get("changed_by");
            if (actor == null) continue;
            List<String> usernames = jdbcTemplate.query(
                    "SELECT username FROM user WHERE id=? OR username=? LIMIT 1",
                    (rs, index) -> rs.getString(1), actor.toString(), actor.toString());
            if (!usernames.isEmpty()) row.put("changed_by", usernames.get(0));
        }
        return rows;
    }

    private NumberSequenceEntity requireSequence(Long id) {
        if (id == null) throw new IllegalArgumentException("Number sequence id is required");
        return numberSequenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Number sequence not found: " + id));
    }

    private void validateSequenceValues(long startNo, long nextNo, long endNo, int allocationSize,
                                        long warningThreshold, NumberSequenceEntity existing) {
        if (startNo < 0) throw new IllegalArgumentException("Start number cannot be negative");
        if (endNo < startNo) throw new IllegalArgumentException("End number must be greater than or equal to start number");
        if (nextNo < startNo) throw new IllegalArgumentException("Next number cannot be below the start number");
        if (endNo < Long.MAX_VALUE && nextNo > endNo + 1L) {
            throw new IllegalArgumentException("Next number cannot be more than one above the end number");
        }
        if (allocationSize < 1 || allocationSize > 10000) {
            throw new IllegalArgumentException("Default allocation size must be between 1 and 10000");
        }
        if (warningThreshold < 0) throw new IllegalArgumentException("Warning threshold cannot be negative");
        if (existing != null) {
            if (startNo != existing.getStartNo()) {
                throw new IllegalArgumentException("Start number cannot be changed after the sequence has been created");
            }
            if (nextNo < existing.getNextNo()) {
                throw new IllegalArgumentException("Next number cannot be decreased because that can create duplicate document numbers");
            }
            if (endNo < existing.getNextNo() - 1L) {
                throw new IllegalArgumentException("End number cannot be below numbers that have already been issued");
            }
        }
    }

    private void applyLegacyValues(NumberRangeEntity entity, LegacyNumberRangeConfigurationRequestDto request,
                                   LegacyNumberRangeConfigurationResponseDto existing) {
        String prefix = request.getPrefix() == null ? (existing == null ? null : existing.getPrefix()) : request.getPrefix().trim();
        if (prefix != null && prefix.length() > 10) {
            throw new IllegalArgumentException("Prefix cannot exceed 10 characters because generated document numbers are limited to 20 characters");
        }
        if (prefix != null && prefix.isEmpty()) prefix = null;

        String startInput = text(request.getStart()) ? request.getStart() : existing == null ? "0000000001" : existing.getStart();
        String currentInput = text(request.getCurrent()) ? request.getCurrent() : existing == null ? "0000000000" : existing.getCurrent();
        String endInput = text(request.getEnd()) ? request.getEnd() : existing == null ? "9999999999" : existing.getEnd();
        long start = trailingNumber(startInput, "Start number");
        long current = trailingNumber(currentInput, "Current number");
        long end = trailingNumber(endInput, "End number");
        if (start > DEFAULT_END_NO || current > DEFAULT_END_NO || end > DEFAULT_END_NO) {
            throw new IllegalArgumentException("Document range values cannot exceed 9999999999");
        }
        if (start < 0 || end < start) throw new IllegalArgumentException("End number must be greater than or equal to start number");
        if (current < Math.max(0L, start - 1L) || current > end) {
            throw new IllegalArgumentException("Current number must be between start minus one and end number");
        }
        if (existing != null && current < trailingNumber(existing.getCurrent(), "Current number")) {
            throw new IllegalArgumentException("Current number cannot be decreased because that can create duplicate document numbers");
        }

        LocalDate validFrom = request.getValidFrom() != null ? request.getValidFrom()
                : existing == null || existing.getValidFrom() == null ? LocalDate.now() : existing.getValidFrom();
        LocalDate validTo = request.getValidTo() != null ? request.getValidTo()
                : existing == null || existing.getValidTo() == null ? LocalDate.of(9999, 12, 31) : existing.getValidTo();
        if (validTo.isBefore(validFrom)) throw new IllegalArgumentException("Valid to date cannot be before valid from date");

        entity.setPrefix(prefix);
        entity.setStart(pad10(start));
        entity.setCurrent((prefix == null ? "" : prefix) + pad10(current));
        entity.setEnd(pad10(end));
        entity.setValidFrom(Date.valueOf(validFrom));
        entity.setValidTo(Date.valueOf(validTo));
    }

    private LegacyNumberRangeConfigurationResponseDto toLegacyResponse(NumberRangeEntity entity) {
        LocalDate validFrom = entity.getValidFrom() == null ? null : new Date(entity.getValidFrom().getTime()).toLocalDate();
        LocalDate validTo = entity.getValidTo() == null ? null : new Date(entity.getValidTo().getTime()).toLocalDate();
        LocalDate today = LocalDate.now();
        boolean active = (validFrom == null || !today.isBefore(validFrom)) && (validTo == null || !today.isAfter(validTo));
        return LegacyNumberRangeConfigurationResponseDto.builder()
                .id(entity.getId())
                .object(entity.getObject())
                .prefix(entity.getPrefix())
                .start(entity.getStart())
                .current(entity.getCurrent())
                .end(entity.getEnd())
                .validFrom(validFrom)
                .validTo(validTo)
                .active(active)
                .build();
    }

    private void audit(String sourceType, String configurationId, String rangeKey, String action,
                       Object previousValue, Object newValue) {
        jdbcTemplate.update(
                "INSERT INTO number_range_configuration_audit (source_type,configuration_id,range_key,action,previous_value,new_value,changed_by) VALUES (?,?,?,?,?,?,?)",
                sourceType, configurationId, rangeKey, action, json(previousValue), json(newValue), actor()
        );
    }

    private String json(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to record number range configuration audit", ex);
        }
    }

    private String actor() {
        if (text(UserContext.getCurrentUserId())) return UserContext.getCurrentUserId();
        if (text(UserContext.getCurrentUser())) return UserContext.getCurrentUser();
        if (text(UserContext.getPlatformUsername())) return UserContext.getPlatformUsername();
        return "SYSTEM";
    }

    private String normalizeKey(String value, int maxLength, String label) {
        if (!text(value)) throw new IllegalArgumentException(label + " is required");
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (normalized.length() > maxLength || !RANGE_KEY_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(label + " may only contain letters, numbers, underscores and hyphens, with a maximum of " + maxLength + " characters");
        }
        return normalized;
    }


    private String normalizePrefix(String value) {
        if (!text(value)) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 32 || !normalized.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("Prefix may only contain letters, numbers, underscores and hyphens, with a maximum of 32 characters");
        }
        return normalized;
    }

    private String normalizeSeparator(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() > 8 || normalized.matches(".*[A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Separator may contain up to 8 non-alphanumeric characters");
        }
        return normalized;
    }

    private void validateFormatting(String prefix, String separator, int paddingLength) {
        if (paddingLength < 0 || paddingLength > 18) {
            throw new IllegalArgumentException("Padding length must be between 0 and 18");
        }
        if (!text(prefix) && text(separator)) {
            throw new IllegalArgumentException("A separator can only be used when a prefix is configured");
        }
    }

    private String normalizeDescription(String value, String seqType) {
        return normalizeDescription(value, seqType, null);
    }

    private String normalizeDescription(String value, String seqType, String fallback) {
        String description = text(value) ? value.trim() : text(fallback) ? fallback.trim() : humanize(seqType);
        if (description.length() > 160) throw new IllegalArgumentException("Description cannot exceed 160 characters");
        return description;
    }

    private String humanize(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1) + " numbers";
    }

    private long trailingNumber(String value, String label) {
        if (!text(value)) throw new IllegalArgumentException(label + " is required");
        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(value.trim());
        if (!matcher.find()) throw new IllegalArgumentException(label + " must end with a numeric value");
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " is too large", ex);
        }
    }

    private String pad10(long value) {
        return String.format(Locale.ROOT, "%010d", value);
    }

    private boolean text(String value) { return value != null && !value.trim().isEmpty(); }
    private String safe(String value) { return value == null ? "" : value; }
    private long valueOr(Long value, long fallback) { return value == null ? fallback : value; }
    private int valueOr(Integer value, int fallback) { return value == null ? fallback : value; }
}
