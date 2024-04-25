package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.AmountDto;
import za.co.mawa.bes.dto.DateDto;
import za.co.mawa.bes.dto.cas.CaseCreateDto;
import za.co.mawa.bes.dto.cas.CaseDto;
import za.co.mawa.bes.dto.cas.CaseQueryDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyCreateDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyDto;
import za.co.mawa.bes.dto.group.society.GroupSocietyQueryDto;
import za.co.mawa.bes.dto.participant.ParticipantDto;
import za.co.mawa.bes.dto.product.ProductDto;
import za.co.mawa.bes.dto.receipt.ReceiptDto;
import za.co.mawa.bes.dto.receipt.ReceiptSearchDto;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.amount.TransactionAmountDto;
import za.co.mawa.bes.dto.transaction.item.TransactionItemDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.exception.*;
import za.co.mawa.bes.utils.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CaseService {
    @Autowired
    TransactionService transactionService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    FieldOptionService fieldOptionService;

    public CaseDto create(CaseCreateDto caseCreateDto) throws PartnerNotFoundException, ProductNotFoundException,
            TransactionItemAddException, TransactionDateAddException, TransactionPartnerAddException {
        TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
        transactionCreateDto.setType(TransactionType.CASE);
        transactionCreateDto.setLocation(caseCreateDto.getCourt());
        transactionCreateDto.setSubType(caseCreateDto.getType());
        transactionCreateDto.setDescription(caseCreateDto.getDescription());
        TransactionDto transactionDto = transactionService.create(transactionCreateDto);
        if (caseCreateDto.getClient() != null) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.CLIENT);
            transactionPartnerDto.setPartner(caseCreateDto.getClient());
            transactionService.addPartner(transactionPartnerDto);
        }
        for (String applicant : caseCreateDto.getApplicants()) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.APPLICANT);
            transactionPartnerDto.setPartner(applicant);
            transactionService.addPartner(transactionPartnerDto);
        }
        for (String defendant : caseCreateDto.getApplicants()) {
            TransactionPartnerDto transactionPartnerDto = new TransactionPartnerDto();
            transactionPartnerDto.setTransaction(transactionDto.getId());
            transactionPartnerDto.setFunction(PartnerFunction.DEFENDANT);
            transactionPartnerDto.setPartner(defendant);
            transactionService.addPartner(transactionPartnerDto);
        }
        CaseDto caseDto = new CaseDto();
        caseDto.setId(transactionDto.getId());
        return caseDto;

    }

    public List<CaseDto> search(CaseQueryDto caseQueryDto) {
        List<CaseDto> caseDtoList = new ArrayList<>();
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.CASE);
        for (String id : transactionService.search(transactionQueryDto)) {
            caseDtoList.add(get(id));
        }
        return caseDtoList;
    }

    public CaseDto get(String id) {
        try {
            TransactionDto transactionDto = transactionService.get(id);
            CaseDto caseDto = new CaseDto();
            caseDto.setId(transactionDto.getId());
            caseDto.setNumber(transactionDto.getNumber());
            caseDto.setDescription(transactionDto.getNumber());
            caseDto.setType(fieldOptionService.getFieldOption(Field.TRANSACTION_TYPE, transactionDto.getType()));
            caseDto.setCourt(fieldOptionService.getFieldOption(Field.COURT, transactionDto.getLocation()));
            List<ParticipantDto> participantDtoList = new ArrayList<>();
            for (TransactionPartnerDto transactionPartnerDto : transactionService.getPartners(id)) {
                try {
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.CLIENT)) {
                        caseDto.setClient(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.APPLICANT)) {
                        caseDto.getApplicants().add(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    if (transactionPartnerDto.getFunction().equals(PartnerFunction.DEFENDANT)) {
                        caseDto.getDefendants().add(partnerService.get(transactionPartnerDto.getPartner()));
                    }
                    ParticipantDto participantDto = new ParticipantDto();
                    participantDto.setPartner(partnerService.get(transactionPartnerDto.getPartner()));
                    participantDto.setFunction(fieldOptionService.getFieldOption(Field.PARTNER_FUNCTION, transactionPartnerDto.getFunction()));
                    participantDtoList.add(participantDto);
                } catch (PartnerNotFoundException e) {

                }
            }
            caseDto.setParticipants(participantDtoList);
            List<DateDto> dateDtoList = new ArrayList<>();
            for (TransactionDateDto transactionDateDto : transactionService.getDates(id)) {
                DateDto dateDto = new DateDto();
                dateDto.setValue(transactionDateDto.getValue());
                dateDto.setType(fieldOptionService.getFieldOption(Field.DATE_TYPE, transactionDateDto.getType()));
                dateDtoList.add(dateDto);
            }
            caseDto.setDates(dateDtoList);
            caseDto.setStatus(fieldOptionService.getFieldOption(Field.TRANSACTION_STATUS, transactionDto.getStatus()));
            caseDto.setStatusReason(fieldOptionService.getFieldOption(Field.STATUS_REASON, transactionDto.getStatusReason()));
            return caseDto;
        } catch (TransactionNotFound e) {
            throw new RuntimeException(e);
        }
    }
}
