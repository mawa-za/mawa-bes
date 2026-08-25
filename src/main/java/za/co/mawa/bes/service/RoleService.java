package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.mawa.bes.dao.RoleDao;
import za.co.mawa.bes.dto.*;
import za.co.mawa.bes.entity.*;
import za.co.mawa.bes.exception.RoleDoesNotExist;
import za.co.mawa.bes.repository.RoleRepository;
import za.co.mawa.bes.repository.RoleWorkcenterRepository;
import za.co.mawa.bes.repository.UserRoleRepository;
import za.co.mawa.bes.utils.Constant;
import za.co.mawa.bes.utils.Conversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class RoleService implements RoleDao {
    private static final String LEGACY_REPORTS_WORKCENTER = "reports";
    private static final Set<String> GRANULAR_REPORT_WORKCENTERS = Set.of(
            "management-membership-overview-report",
            "management-memberships-by-plan-report",
            "operational-premium-performance-report",
            "operational-claims-activity-report",
            "operational-customer-money-received-report",
            "operational-cashier-collections-report",
            "operational-deposits-summary-report",
            "operational-undeposited-collections-report",
            "management-collections-deposits-reconciliation-report",
            "management-supplier-payments-summary-report",
            "management-payments-by-service-report",
            "operational-supplier-payment-detail-report"
    );
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    RoleWorkcenterRepository roleWorkcenterRepository;
    @Autowired
    WorkcenterService workcenterService;
    @Autowired
    UserService userService;
    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    UserAccessService userAccessService;

    @Override
    public void create(RoleDto roleDto) throws Exception {
        try {
            RoleEntity existing = roleRepository.findById(roleDto.getId()).orElse(null);
            boolean existingPrivileged = existing != null && (Boolean.TRUE.equals(existing.getSystemRole())
                    || Boolean.TRUE.equals(existing.getProtectedRole())
                    || Boolean.TRUE.equals(existing.getAccessAllWorkcentres()));
            boolean privilegedChange = Boolean.TRUE.equals(roleDto.getSystemRole())
                    || Boolean.TRUE.equals(roleDto.getProtectedRole())
                    || Boolean.TRUE.equals(roleDto.getAccessAllWorkcentres());
            if ((existingPrivileged || privilegedChange) && !userAccessService.isProtectedAdministrator()) {
                throw new SecurityException("Only a protected tenant administrator can create or change protected/all-access roles");
            }
            if (existing != null && Boolean.TRUE.equals(existing.getProtectedRole())) {
                if (Boolean.TRUE.equals(existing.getAccessAllWorkcentres()) && !Boolean.TRUE.equals(roleDto.getAccessAllWorkcentres())) {
                    throw new IllegalStateException("PROTECTED_ROLE: Access-all cannot be removed from a protected role");
                }
                roleDto.setProtectedRole(existing.getProtectedRole());
                roleDto.setSystemRole(existing.getSystemRole());
            }
            if (roleDto.getValidFrom() == null) roleDto.setValidFrom(new Date());
            if (roleDto.getValidTo() == null) roleDto.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            roleRepository.save(dtoToEntity(roleDto));
        } catch (IllegalStateException | SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new Exception("Failed to save role " + roleDto.getId() + ": " + exception.getMessage(), exception);
        }
    }

    @Override
    public List<RoleDto> getAll() {
        List<RoleDto> roleDtoList = new ArrayList<>();
        for (RoleEntity roleEntity : roleRepository.findAll()) {
            RoleDto roleDto = new RoleDto();
            roleDto.setId(roleEntity.getId());
            roleDto.setDescription(roleEntity.getDescription());
            roleDto.setValidFrom(roleEntity.getValidFrom());
            roleDto.setValidTo(roleEntity.getValidTo());
            roleDto.setSystemRole(roleEntity.getSystemRole());
            roleDto.setProtectedRole(roleEntity.getProtectedRole());
            roleDto.setAccessAllWorkcentres(roleEntity.getAccessAllWorkcentres());
            roleDtoList.add(roleDto);
        }
        return roleDtoList;
    }

    public RoleOutboundDto get(String id) {
        RoleEntity roleEntity = roleRepository.getById(id);
        RoleOutboundDto roleOutboundDto = new RoleOutboundDto();
        roleOutboundDto.setId(roleEntity.getId());
        roleOutboundDto.setName(roleEntity.getDescription());
        roleOutboundDto.setDescription(roleEntity.getDescription());
        roleOutboundDto.setValidFrom(roleEntity.getValidFrom());
        roleOutboundDto.setValidTo(roleEntity.getValidTo());
        roleOutboundDto.setSystemRole(roleEntity.getSystemRole());
        roleOutboundDto.setProtectedRole(roleEntity.getProtectedRole());
        roleOutboundDto.setAccessAllWorkcentres(roleEntity.getAccessAllWorkcentres());
        return roleOutboundDto;
    }

    @Override
    public List<RoleWorkcenterDto> getRoleWorkcenters(String role) throws RoleDoesNotExist {
        RoleEntity requestedRole = roleRepository.findById(role).orElse(null);
        if ((requestedRole != null && Boolean.TRUE.equals(requestedRole.getAccessAllWorkcentres())) || role.equals("SYSTEM")) {
            int i = 1;
            List<RoleWorkcenterDto> roleWorkcenterDtoList = new ArrayList<>();
            List<WorkcenterDto> workcenterDtoList = workcenterService.getAll();
            for (WorkcenterDto workcenterDto : workcenterDtoList) {
                RoleWorkcenterDto roleWorkcenterDto = new RoleWorkcenterDto();
                roleWorkcenterDto.setPosition(i);
                roleWorkcenterDto.setWorkcenter(workcenterDto);
                roleWorkcenterDtoList.add(roleWorkcenterDto);
                i++;
            }
            return roleWorkcenterDtoList;
        } else {
            List<RoleWorkcenterDto> workcenterDtoList = new ArrayList<>();
            List<RoleWorkcenterEntity> roleWorkcenterEntities = roleWorkcenterRepository.findRoleWorkcenters(role);
            for (RoleWorkcenterEntity roleWorkcenterEntity : roleWorkcenterEntities) {
                String workcenter = roleWorkcenterEntity.getRoleWorkcenterPKEntity().getWorkcenter();
                try {
                    RoleWorkcenterDto roleWorkcenterDto = new RoleWorkcenterDto();
                    WorkcenterDto workcenterDto = workcenterService.getById(workcenter);
                    roleWorkcenterDto.setPosition(roleWorkcenterEntity.getPosition());
                    roleWorkcenterDto.setWorkcenter(workcenterDto);
                    workcenterDtoList.add(roleWorkcenterDto);
                } catch (RoleDoesNotExist ignored) {
                    // Keep the home screen usable when a role still contains an old or disabled workcenter code.
                }
            }
            return workcenterDtoList;
        }
    }

    @Override
    public void addWorkcenter(RoleWorkcenterCreateDto roleWorkcenterCreateDto) throws Exception {
        try {
            RoleEntity role = roleRepository.findById(roleWorkcenterCreateDto.getRole()).orElse(null);
            if (role != null && (Boolean.TRUE.equals(role.getProtectedRole()) || Boolean.TRUE.equals(role.getSystemRole()))
                    && !userAccessService.isProtectedAdministrator()) {
                throw new SecurityException("Only a protected tenant administrator can change protected role workcentres");
            }
            if (role != null && Boolean.TRUE.equals(role.getAccessAllWorkcentres())) {
                throw new IllegalStateException("ACCESS_ALL_ROLE: Workcentre assignments are automatic for this role");
            }
            RoleWorkcenterPKEntity roleWorkcenterPKEntity = new RoleWorkcenterPKEntity();
            roleWorkcenterPKEntity.setRole(roleWorkcenterCreateDto.getRole());
            roleWorkcenterPKEntity.setWorkcenter(roleWorkcenterCreateDto.getWorkcenter());
            RoleWorkcenterEntity roleWorkcenterEntity = new RoleWorkcenterEntity();
            roleWorkcenterEntity.setRoleWorkcenterPKEntity(roleWorkcenterPKEntity);
            roleWorkcenterEntity.setPosition(roleWorkcenterCreateDto.getPosition());
            roleWorkcenterRepository.save(roleWorkcenterEntity);
            if (isGranularReportWorkcenter(roleWorkcenterCreateDto.getWorkcenter())) {
                ensureLegacyReportsMarker(
                        roleWorkcenterCreateDto.getRole(),
                        roleWorkcenterCreateDto.getPosition()
                );
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new Exception("Failed to add role workcentre: " + exception.getMessage(), exception);
        }
    }

    @Override
    public boolean deleteWorkcenter(RoleWorkcenterPKEntity entity) throws Exception {
        try {
            RoleEntity role = roleRepository.findById(entity.getRole()).orElse(null);
            if (role != null && (Boolean.TRUE.equals(role.getProtectedRole()) || Boolean.TRUE.equals(role.getSystemRole()))
                    && !userAccessService.isProtectedAdministrator()) {
                throw new SecurityException("Only a protected tenant administrator can change protected role workcentres");
            }
            if (role != null && Boolean.TRUE.equals(role.getAccessAllWorkcentres())) {
                throw new IllegalStateException("ACCESS_ALL_ROLE: Workcentre assignments cannot be removed from this role");
            }
            roleWorkcenterRepository.deleteById(entity);
            if (isGranularReportWorkcenter(entity.getWorkcenter())) {
                removeLegacyReportsMarkerWhenUnused(entity.getRole());
            }
            return true;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean isGranularReportWorkcenter(String workcenter) {
        return workcenter != null && GRANULAR_REPORT_WORKCENTERS.contains(workcenter.trim().toLowerCase());
    }

    /**
     * Keep the historic reports permission as a hidden compatibility marker.
     * The reporting service may still use it while ERP access is controlled by
     * the new per-report workcentres.
     */
    private void ensureLegacyReportsMarker(String role, Integer requestedPosition) {
        RoleWorkcenterPKEntity markerPk = new RoleWorkcenterPKEntity();
        markerPk.setRole(role);
        markerPk.setWorkcenter(LEGACY_REPORTS_WORKCENTER);
        if (roleWorkcenterRepository.existsById(markerPk)) return;

        RoleWorkcenterEntity marker = new RoleWorkcenterEntity();
        marker.setRoleWorkcenterPKEntity(markerPk);
        marker.setPosition(requestedPosition == null ? 999 : requestedPosition);
        roleWorkcenterRepository.save(marker);
    }

    private void removeLegacyReportsMarkerWhenUnused(String role) {
        boolean anyGranularReport = roleWorkcenterRepository.findRoleWorkcenters(role).stream()
                .map(item -> item.getRoleWorkcenterPKEntity().getWorkcenter())
                .anyMatch(this::isGranularReportWorkcenter);
        if (anyGranularReport) return;

        RoleWorkcenterPKEntity markerPk = new RoleWorkcenterPKEntity();
        markerPk.setRole(role);
        markerPk.setWorkcenter(LEGACY_REPORTS_WORKCENTER);
        if (roleWorkcenterRepository.existsById(markerPk)) {
            roleWorkcenterRepository.deleteById(markerPk);
        }
    }

    @Override
    @Transactional
    public boolean deleteRole(String role) throws Exception {
        RoleEntity roleEntity = roleRepository.findById(role).orElseThrow(() -> new RoleDoesNotExist());
        if (Boolean.TRUE.equals(roleEntity.getProtectedRole()) || Boolean.TRUE.equals(roleEntity.getSystemRole())) {
            throw new IllegalStateException("PROTECTED_ROLE: System roles cannot be deleted");
        }
        for (RoleWorkcenterEntity assignment : roleWorkcenterRepository.findRoleWorkcenters(role)) {
            roleWorkcenterRepository.delete(assignment);
        }
        for (UserRoleEntity userRole : userRoleRepository.findRoles(role)) {
            userRoleRepository.delete(userRole);
        }
        roleRepository.delete(roleEntity);
        return true;
    }

    private RoleDto entityToDto(RoleEntity roleEntity) {
        RoleDto roleDto = new RoleDto();
        roleDto.setId(roleEntity.getId());
        roleDto.setDescription(roleEntity.getDescription());
        roleDto.setValidFrom(roleEntity.getValidFrom());
        roleDto.setValidTo(roleEntity.getValidTo());
        roleDto.setSystemRole(roleEntity.getSystemRole());
        roleDto.setProtectedRole(roleEntity.getProtectedRole());
        roleDto.setAccessAllWorkcentres(roleEntity.getAccessAllWorkcentres());
        return roleDto;
    }

    private RoleEntity dtoToEntity(RoleDto roleDto) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(roleDto.getId());
        roleEntity.setDescription(roleDto.getDescription());
        roleEntity.setSystemRole(Boolean.TRUE.equals(roleDto.getSystemRole()));
        roleEntity.setProtectedRole(Boolean.TRUE.equals(roleDto.getProtectedRole()));
        roleEntity.setAccessAllWorkcentres(Boolean.TRUE.equals(roleDto.getAccessAllWorkcentres()));
        roleEntity.setValidFrom(roleDto.getValidFrom());
        roleEntity.setValidTo(roleDto.getValidTo());
        return roleEntity;
    }
}
