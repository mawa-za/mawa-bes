package za.co.mawa.bes.service.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.TransactionViewDto;
import za.co.mawa.bes.entity.transaction.TransactionViewEntity;
import za.co.mawa.bes.entity.v2.MembershipDependentEntity;
import za.co.mawa.bes.entity.v2.MembershipEntity;
import za.co.mawa.bes.repository.v2.MembershipRepository;
import za.co.mawa.bes.service.DependentService;
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

    @Scheduled(cron = "0 */10 * * * *")
    public void migrateMemberships() {
        TransactionViewDto transactionViewDto = new TransactionViewDto();
        transactionViewDto.setType(TransactionType.MEMBERSHIP);
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

            }else{
               List<MembershipDependentEntity> dependentEntityList =  membershipDependentService.getDependentsByMembershipId(membership.getOldId());
               for(PartnerDto dependent : dependentList){
                   boolean isDependentExist = dependentEntityList.stream().anyMatch(e -> e.getDependentPartnerId().equals(dependent.getId()));
                   if(!isDependentExist){
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
    }
}
