package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.partner.PartnerDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.PartnerNotFoundException;
import za.co.mawa.bes.exception.TransactionPartnerAddException;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.PartnerFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DependentService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;

    public ArrayList<PartnerDto> get(String id) {
        List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id);
        ArrayList<PartnerDto> dependentDtoList = new ArrayList<>();
        for(TransactionPartnerDto partners : transactionPartnerDtoList){
            if(partners.getFunction().equalsIgnoreCase(PartnerFunction.DEPENDENT)){
            try {
                dependentDtoList.add(partnerService.get(partners.getPartner()));
            } catch (Exception ex){

            }
            }
        }
        return dependentDtoList;
    }
    public void add(DependentDto dependentDto) {
        try {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(dependentDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.DEPENDENT);
            transactionPartnerDto.setPartner(dependentDto.getId());
            transactionService.addPartner(transactionPartnerDto);
        } catch (TransactionPartnerAddException e) {
            throw new RuntimeException(e);
        }
    }

    public void remove(DependentDto dependentDto) {

    }
}
