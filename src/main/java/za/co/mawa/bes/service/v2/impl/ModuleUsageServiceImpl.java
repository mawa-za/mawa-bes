package za.co.mawa.bes.service.v2.impl;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.moduleusage.ModuleUsageResponse;
import za.co.mawa.bes.dto.moduleusage.TrackModuleUsageRequest;
import za.co.mawa.bes.entity.v2.ModuleUsageEventEntity;
import za.co.mawa.bes.entity.v2.UserModuleUsageEntity;
import za.co.mawa.bes.repository.v2.ModuleUsageEventRepository;
import za.co.mawa.bes.repository.v2.UserModuleUsageRepository;
import za.co.mawa.bes.service.v2.ModuleUsageService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModuleUsageServiceImpl implements ModuleUsageService {

    private final ModuleUsageEventRepository moduleUsageEventRepository;
    private final UserModuleUsageRepository userModuleUsageRepository;

    public ModuleUsageServiceImpl(
            ModuleUsageEventRepository moduleUsageEventRepository,
            UserModuleUsageRepository userModuleUsageRepository
    ) {
        this.moduleUsageEventRepository = moduleUsageEventRepository;
        this.userModuleUsageRepository = userModuleUsageRepository;
    }

    @Override
    @Transactional
    public ModuleUsageResponse trackUsage(TrackModuleUsageRequest request) {
        String userId = resolveUserId(request.getUserId());
        String loggedInUser = getAuthenticatedUser();
        LocalDateTime now = LocalDateTime.now();

        ModuleUsageEventEntity event = new ModuleUsageEventEntity();
        event.setUserId(userId);
        event.setModuleCode(request.getModuleCode());
        event.setModuleName(request.getModuleName());
        event.setModulePath(request.getModulePath());
        event.setWorkcenterId(request.getWorkcenterId());
        event.setUsedAt(now);
        event.setCreatedBy(loggedInUser);
        moduleUsageEventRepository.save(event);

        UserModuleUsageEntity aggregate = userModuleUsageRepository
                .findByUserIdAndModuleCode(userId, request.getModuleCode())
                .orElseGet(() -> {
                    UserModuleUsageEntity entity = new UserModuleUsageEntity();
                    entity.setUserId(userId);
                    entity.setModuleCode(request.getModuleCode());
                    entity.setUsageCount(0L);
                    entity.setCreatedBy(loggedInUser);
                    return entity;
                });

        aggregate.setModuleName(request.getModuleName());
        aggregate.setModulePath(request.getModulePath());
        aggregate.setWorkcenterId(request.getWorkcenterId());
        aggregate.setUpdatedBy(loggedInUser);
        aggregate.increaseUsage(now);

        return toResponse(userModuleUsageRepository.save(aggregate));
    }

    @Override
    public List<ModuleUsageResponse> getFrequentlyUsedModules(String userId, int limit) {
        String resolvedUserId = resolveUserId(userId);
        int safeLimit = normalizeLimit(limit);

        return userModuleUsageRepository
                .findByUserIdOrderByUsageCountDescLastUsedAtDesc(resolvedUserId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ModuleUsageResponse> getRecentlyUsedModules(String userId, int limit) {
        String resolvedUserId = resolveUserId(userId);
        int safeLimit = normalizeLimit(limit);

        return userModuleUsageRepository
                .findByUserIdOrderByLastUsedAtDesc(resolvedUserId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void resetUserUsage(String userId) {
        String resolvedUserId = resolveUserId(userId);
        moduleUsageEventRepository.deleteByUserId(resolvedUserId);
        userModuleUsageRepository.deleteByUserId(resolvedUserId);
    }

    private ModuleUsageResponse toResponse(UserModuleUsageEntity entity) {
        return new ModuleUsageResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getModuleCode(),
                entity.getModuleName(),
                entity.getModulePath(),
                entity.getWorkcenterId(),
                entity.getUsageCount(),
                entity.getFirstUsedAt(),
                entity.getLastUsedAt()
        );
    }

    private String resolveUserId(String requestUserId) {
        if (requestUserId != null && !requestUserId.isBlank()) {
            return requestUserId;
        }

        String authenticatedUser = getAuthenticatedUser();

        if (authenticatedUser == null || authenticatedUser.isBlank() || "anonymousUser".equals(authenticatedUser)) {
            throw new IllegalArgumentException("Unable to resolve userId. Provide userId or authenticate the request.");
        }

        return authenticatedUser;
    }

    private String getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 8;
        }
        return Math.min(limit, 50);
    }
}
