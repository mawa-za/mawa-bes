package za.co.mawa.bes.service;

import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.claim.ClaimDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.TransactionNotFound;
import za.co.mawa.bes.utils.DateType;
import za.co.mawa.bes.utils.PartnerFunction;

import java.util.Objects;

@Service
public class ClaimService extends TransactionService{
    private ClaimDto getClaimData(String id) throws TransactionNotFound {
//        try {
//            TransactionDto transactionDto = transactionService.get(id);
            ClaimDto claimDto = new ClaimDto();
//            claimDto.setId(transactionDto.getId());
//            claimDto.setNumber(transactionDto.getNumber());
//            claimDto.setType(transactionDto.getSubType());
//            claimDto.setStatus(transactionDto.getStatus());
//            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
//                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.MAINMEMBER)) {
//                    claimDto.setMemberId(transactionPartnerDto.getPartner());
//                }
//                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.DECEASED)) {
//                    claimDto.setDeceasedId(transactionPartnerDto.getPartner());
//                }
//                if (Objects.equals(transactionPartnerDto.getFunction(), PartnerFunction.CLAIMANT)) {
//                    claimDto.setDeceasedId(transactionPartnerDto.getPartner());
//                }
//            }
//            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
//                if (Objects.equals(transactionDateDto.getType(), DateType.CREATED)) {
//                    claimDto.setCreationDate(transactionDateDto.getValue());
//                }
//            }
            return claimDto;
//        }catch(TransactionNotFound exception){
//            throw new TransactionNotFound("Claim not found");
//        }
    }
}
