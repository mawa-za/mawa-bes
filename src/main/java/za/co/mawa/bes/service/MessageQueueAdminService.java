package za.co.mawa.bes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dto.v2.message.MessageQueueAdminDto;
import za.co.mawa.bes.dto.v2.message.MessageQueueAdminListResponse;
import za.co.mawa.bes.dto.v2.message.MessageQueueScheduleSettingsDto;
import za.co.mawa.bes.entity.MessageQueueEntity;
import za.co.mawa.bes.repository.MessageQueueRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageQueueAdminService {
    private final MessageQueueRepository repository;
    private final MessageConsumerService messageConsumerService;
    private final SettingService settingService;

    private static final String QUEUE_GROUP = "MESSAGE-QUEUE";

    public MessageQueueAdminListResponse search(String type, String status, String reference, int page, int size) {
        List<MessageQueueAdminDto> filtered = repository.findAll().stream()
                .filter(e -> isBlank(type) || safe(e.getType()).equalsIgnoreCase(type))
                .filter(e -> matchesStatus(e, status))
                .filter(e -> isBlank(reference)
                        || safe(e.getReferenceId()).toLowerCase(Locale.ROOT).contains(reference.toLowerCase(Locale.ROOT))
                        || safe(e.getReferenceNo()).toLowerCase(Locale.ROOT).contains(reference.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(MessageQueueEntity::getNextAttemptAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .collect(Collectors.toList());

        int safeSize = size <= 0 ? 50 : Math.min(size, 200);
        int from = Math.max(0, page) * safeSize;
        int to = Math.min(from + safeSize, filtered.size());
        List<MessageQueueAdminDto> items = from >= filtered.size() ? List.of() : filtered.subList(from, to);
        return new MessageQueueAdminListResponse(items, filtered.size());
    }

    public MessageQueueAdminDto get(Long id) {
        return toDto(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Message queue item not found: " + id)));
    }

    @Transactional
    public MessageQueueAdminDto retry(Long id) {
        MessageQueueEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message queue item not found: " + id));
        entity.setProcessed(false);
        entity.setRetryCount(0);
        entity.setNextAttemptAt(LocalDateTime.now());
        return toDto(repository.save(entity));
    }

    @Transactional
    public MessageQueueAdminDto markProcessed(Long id) {
        MessageQueueEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message queue item not found: " + id));
        entity.setProcessed(true);
        return toDto(repository.save(entity));
    }

    public int processNow() {
        return messageConsumerService.processCurrentTenant();
    }

    public MessageQueueScheduleSettingsDto getScheduleSettings() {
        return MessageQueueScheduleSettingsDto.builder()
                .enabled(messageConsumerService.isSchedulerEnabled())
                .intervalSeconds(messageConsumerService.getSchedulerIntervalSeconds())
                .batchSize(messageConsumerService.getBatchSize())
                .lastRunAt(messageConsumerService.getLastRunAt() == null ? null : messageConsumerService.getLastRunAt().toString())
                .nextRunAt(messageConsumerService.getNextRunAt() == null ? null : messageConsumerService.getNextRunAt().toString())
                .build();
    }

    public MessageQueueScheduleSettingsDto updateScheduleSettings(MessageQueueScheduleSettingsDto request) {
        int interval = request.getIntervalSeconds() <= 0 ? 60 : Math.max(30, request.getIntervalSeconds());
        int batch = request.getBatchSize() <= 0 ? 10 : Math.max(1, Math.min(request.getBatchSize(), 100));
        settingService.upsertSetting("ENABLED", QUEUE_GROUP, String.valueOf(request.isEnabled()));
        settingService.upsertSetting("INTERVAL-SECONDS", QUEUE_GROUP, String.valueOf(interval));
        settingService.upsertSetting("BATCH-SIZE", QUEUE_GROUP, String.valueOf(batch));
        return getScheduleSettings();
    }

    public MessageQueueScheduleSettingsDto startScheduler() {
        settingService.upsertSetting("ENABLED", QUEUE_GROUP, "true");
        return getScheduleSettings();
    }

    public MessageQueueScheduleSettingsDto stopScheduler() {
        settingService.upsertSetting("ENABLED", QUEUE_GROUP, "false");
        return getScheduleSettings();
    }

    private boolean matchesStatus(MessageQueueEntity entity, String status) {
        if (isBlank(status) || "ALL".equalsIgnoreCase(status)) return true;
        String current = statusOf(entity);
        return current.equalsIgnoreCase(status);
    }

    private MessageQueueAdminDto toDto(MessageQueueEntity entity) {
        return MessageQueueAdminDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .referenceId(entity.getReferenceId())
                .referenceNo(entity.getReferenceNo())
                .payload(entity.getPayload())
                .processed(entity.isProcessed())
                .status(statusOf(entity))
                .retryCount(entity.getRetryCount())
                .nextAttemptAt(entity.getNextAttemptAt())
                .build();
    }

    private String statusOf(MessageQueueEntity entity) {
        if (entity.isProcessed() && entity.getRetryCount() > 3) return "FAILED";
        if (entity.isProcessed()) return "PROCESSED";
        if (entity.getNextAttemptAt() != null && entity.getNextAttemptAt().isAfter(LocalDateTime.now())) return "RETRY_WAIT";
        return "PENDING";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
