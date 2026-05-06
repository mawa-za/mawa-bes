package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.configuration.context.TenantContext;
import za.co.mawa.bes.dto.TenantDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.entity.v2.MembershipPlanEntity;
import za.co.mawa.bes.repository.ProductRepository;
import za.co.mawa.bes.repository.v2.MembershipPlanRepository;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.DependentService;
import za.co.mawa.bes.service.ProductService;
import za.co.mawa.bes.service.TenantAdminService;
import za.co.mawa.bes.service.TransactionService;
import za.co.mawa.bes.utils.TransactionType;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class MigrateService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    @Qualifier("MembershipServiceV2")
    MembershipService membershipService;
    @Autowired
    MembershipDependentService membershipDependentService;
    @Autowired
    MembershipRepository membershipRepository;
    @Autowired
    DependentService dependentService;
    @Autowired
    TenantAdminService tenantAdminService;
    @Autowired
    ProductService productService;
    @Autowired
    MembershipPlanRepository membershipPlanRepository;
    @Autowired
    MembershipPlanService membershipPlanService;

//    @Scheduled(cron = "0 */10 * * * *")
    public void migrateMembershipPlans() {
            try {
                List<ProductDto> productDtoList = productService.query("MEMBERSHIP","%");
                for (ProductDto productDto : productDtoList) {
                    MembershipPlanEntity membershipPlanEntity = membershipPlanRepository.findByOldId(productDto.getId()).orElse(null);
                    if (membershipPlanEntity == null) {
                        membershipPlanEntity = new MembershipPlanEntity();
                        membershipPlanEntity.setPlanCode(productDto.getCode());
                        membershipPlanEntity.setOldId(productDto.getId());
                        membershipPlanEntity.setDescription(productDto.getDescription());
                        membershipPlanEntity.setName(productDto.getDescription());
                        membershipPlanEntity.setCurrency("ZAR");
                        membershipPlanEntity.setMaxDependents(15);
                        membershipPlanEntity.setActive(true);
                        membershipPlanEntity.setPremiumCents(19000L);
                        membershipPlanService.createPlan(membershipPlanEntity);
                    }

                }
            } catch (Exception e) {
                System.err.println("Error processing tenant "+ e.getMessage());
            }

    }


    @Scheduled(cron = "0 */10 * * * *")
    public void migrateMemberships() {
        TransactionViewDto transactionViewDto = new TransactionViewDto();
        transactionViewDto.setType(TransactionType.MEMBERSHIP);
        for (TenantDto tenant : tenantAdminService.getAll()) {
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                migrateMembershipPlans();
                List<TransactionViewEntity> transactionViewEntities = transactionService.searchV2(transactionViewDto);
                for (TransactionViewEntity transactionViewEntity : transactionViewEntities) {
                    ArrayList<PartnerDto> dependentList = dependentService.get(transactionViewEntity.getTransactionId());
                    MembershipEntity membership = membershipRepository.findByOldId(transactionViewEntity.getTransactionId()).orElse(null);
                    if (membership == null) {
                        membership = new MembershipEntity();
                        membership.setId(transactionViewEntity.getTransactionId());
                        membership.setMemberId(transactionViewEntity.getMainPartnerId());
                        membership.setMembershipNo(transactionViewEntity.getTransactionNumber());
                        membership.setPlanId(transactionViewEntity.getProductId());
                        membership.setStatus(transactionViewEntity.getTransactionStatus());
                        membership.setStartDate(transactionViewEntity.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        membership.setJoinDate(transactionViewEntity.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                        membership.setCreatedAt(transactionViewEntity.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                        membership.setCreatedBy(transactionViewEntity.getCreatedById());
                        membership.setUpdatedBy(transactionViewEntity.getChangedById());
                        MembershipEntity createdMembership = membershipService.createMembership(membership);
                        for (PartnerDto dependent : dependentList) {
                            MembershipDependentEntity membershipDependentEntity = new MembershipDependentEntity();
                            membershipDependentEntity.setMembershipId(createdMembership.getId());
                            membershipDependentEntity.setDependentPartnerId(dependent.getId());
                            membershipDependentEntity.setUpdatedAt(transactionViewEntity.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                            membershipDependentEntity.setCreatedAt(transactionViewEntity.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                            membershipDependentEntity.setCreatedBy(transactionViewEntity.getCreatedById());
                            membershipDependentEntity.setUpdatedBy(transactionViewEntity.getChangedById());
                            membershipDependentService.addDependent(createdMembership.getId(), membershipDependentEntity);
                        }

                    } else {
                        List<MembershipDependentEntity> dependentEntityList = membershipDependentService.getDependentsByMembershipId(membership.getOldId());
                        for (PartnerDto dependent : dependentList) {
                            boolean isDependentExist = dependentEntityList.stream().anyMatch(e -> e.getDependentPartnerId().equals(dependent.getId()));
                            if (!isDependentExist) {
                                MembershipDependentEntity membershipDependentEntity = new MembershipDependentEntity();
                                membershipDependentEntity.setMembershipId(membership.getId());
                                membershipDependentEntity.setDependentPartnerId(dependent.getId());
                                membershipDependentEntity.setUpdatedAt(transactionViewEntity.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                                membershipDependentEntity.setCreatedAt(transactionViewEntity.getCreationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                                membershipDependentEntity.setCreatedBy(transactionViewEntity.getCreatedById());
                                membershipDependentEntity.setUpdatedBy(transactionViewEntity.getChangedById());
                                membershipDependentService.addDependent(membership.getId(), membershipDependentEntity);
                            }
                        }
                    }

                }
            } catch (Exception e) {
                System.err.println("Error processing tenant " + tenant + ": " + e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}