package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dao.ServiceRequestDao;
import za.co.mawa.bes.dao.TransactionDao;
import za.co.mawa.bes.dto.service.request.*;
import za.co.mawa.bes.dto.transaction.TransactionCreateDto;
import za.co.mawa.bes.dto.transaction.TransactionDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryDto;
import za.co.mawa.bes.dto.transaction.TransactionQueryResultDto;
import za.co.mawa.bes.dto.transaction.partner.TransactionPartnerDto;
import za.co.mawa.bes.utils.PartnerFunction;
import za.co.mawa.bes.utils.Status;
import za.co.mawa.bes.utils.TransactionType;

import java.util.List;

@Service
public class ServiceRequestService implements ServiceRequestDao {
    @Autowired
    TransactionService transactionService;
    @Override
    public ServiceRequestDto create(ServiceRequestCreateDto serviceRequestCreateDto) {
        try {
            TransactionCreateDto transactionCreateDto = new TransactionCreateDto();
            transactionCreateDto.setType(TransactionType.SERVICE_REQUEST);
            transactionCreateDto.setDescription(serviceRequestCreateDto.getDescription());
            transactionCreateDto.setSubType(serviceRequestCreateDto.getCategory());
            transactionCreateDto.setCategory(serviceRequestCreateDto.getCategory());
            transactionCreateDto.setPriority(serviceRequestCreateDto.getPriority());
            transactionCreateDto.setCustomerId(serviceRequestCreateDto.getCustomerId());
            transactionCreateDto.setStatus(Status.NEW);
            TransactionDto transactionDto = transactionService.create(transactionCreateDto);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public ServiceRequestDto edit(ServiceRequestEditDto serviceRequestEditDto) {
        return null;
    }

    @Override
    public List<ServiceRequestQueryResultDto> search(ServiceRequestQueryDto serviceRequestQueryDto) {
        TransactionQueryDto transactionQueryDto = new TransactionQueryDto();
        transactionQueryDto.setType(TransactionType.SERVICE_REQUEST);
        transactionQueryDto.setStatus(serviceRequestQueryDto.getStatus());
        List<TransactionQueryResultDto> transactionDaoList = transactionService.search(transactionQueryDto);
        return null;
    }

    @Override
    public ServiceRequestDto get(String id) {
        return null;
    }

    @Override
    public void delete(String id) {

    }

}
