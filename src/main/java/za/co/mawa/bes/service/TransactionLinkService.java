package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.transaction.link.TransactionLinkOutboundDto;
import za.co.mawa.bes.entity.transaction.TransactionLinkEntity;
import za.co.mawa.bes.repository.TransactionLinkRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionLinkService {
    @Autowired
    TransactionLinkRepository transactionLinkRepository;

   public List<TransactionLinkOutboundDto> getParent(String child){
       List<TransactionLinkOutboundDto> transactionLinkOutboundDtoList = new ArrayList<>();
       for(TransactionLinkEntity transactionLinkEntity:transactionLinkRepository.getParent(child)){
           transactionLinkOutboundDtoList.add(entityToDto(transactionLinkEntity));
       }
       return transactionLinkOutboundDtoList;
    }
    public List<TransactionLinkOutboundDto> getChildren(String parent){
        List<TransactionLinkOutboundDto> transactionLinkOutboundDtoList = new ArrayList<>();
        for(TransactionLinkEntity transactionLinkEntity:transactionLinkRepository.getChildren(parent)){
            transactionLinkOutboundDtoList.add(entityToDto(transactionLinkEntity));
        }
        return transactionLinkOutboundDtoList;
    }
    private TransactionLinkOutboundDto entityToDto(TransactionLinkEntity transactionLinkEntity){
        TransactionLinkOutboundDto transactionLinkOutboundDto = new TransactionLinkOutboundDto();
        transactionLinkOutboundDto.setParent(transactionLinkEntity.getTransactionLinkPKEntity().getTransaction1());
        transactionLinkOutboundDto.setChild(transactionLinkEntity.getTransactionLinkPKEntity().getTransaction2());
        transactionLinkOutboundDto.setType(transactionLinkEntity.getTransactionLinkPKEntity().getType());
        return transactionLinkOutboundDto;
    }
}
