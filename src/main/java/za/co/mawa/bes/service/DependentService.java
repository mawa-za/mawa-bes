package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.DependentDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.TransactionPartnerAddException;
import za.co.mawa.bes.utils.Field;
import za.co.mawa.bes.utils.PartnerFunction;

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

    public List<DependentDto> get(String id) {
        List<TransactionPartnerDto> transactionPartnerDtoList = transactionService.getPartners(id).stream()
                .filter(a -> Objects.equals(a.getFunction(), PartnerFunction.DEPENDENT))
                .toList();
        List<DependentDto> dependentDtoList = transactionPartnerDtoList.stream()
                .map(TransactionPartnerDto::getPartner)
                .map(partnerService::getOptional)
                .filter(Objects::nonNull)
                .map(partnerDto -> {
                    DependentDto dependentDto = new DependentDto();
                    dependentDto.setId(partnerDto.getId());
                    if (partnerDto.getTitle() != null) {
                        String title = fieldOptionService.getFieldOptionDescription(Field.TITLE, partnerDto.getTitle());
                        if (title != null) {
                            dependentDto.setTitle(title);
                        }
                    }
                    if (partnerDto.getGender() != null) {
                        String gender = fieldOptionService.getFieldOptionDescription(Field.GENDER, partnerDto.getGender());
                        if (gender != null) {
                            dependentDto.setGender(gender);
                        }
                    }
                    dependentDto.setIdType(partnerDto.getIdType());
                    dependentDto.setIdNumber(partnerDto.getIdNumber());
                    dependentDto.setName1(partnerDto.getName1());
                    dependentDto.setName2(partnerDto.getName2());
                    dependentDto.setName3(partnerDto.getName3());
                    return dependentDto;
                })
                .collect(Collectors.toList());
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
