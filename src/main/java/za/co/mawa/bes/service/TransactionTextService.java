package za.co.mawa.bes.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.mawa.bes.dto.transaction.note.TransactionNoteDto;
import za.co.mawa.bes.dto.transaction.text.TransactionTextDto;
import za.co.mawa.bes.entity.transaction.TransactionTextEntity;
import za.co.mawa.bes.entity.transaction.TransactionTextPKEntity;
import za.co.mawa.bes.repository.TransactionRepository;
import za.co.mawa.bes.repository.TransactionTextRepository;

@Service
public class TransactionTextService {
    @Autowired
    TransactionTextRepository transactionTextRepository;
    public void add(TransactionTextDto transactionTextDto){
        TransactionTextPKEntity transactionTextPKEntity = new TransactionTextPKEntity();
        transactionTextPKEntity.setTransaction(transactionTextDto.getTransaction());
        transactionTextPKEntity.setType(transactionTextDto.getType());
        TransactionTextEntity transactionTextEntity = new TransactionTextEntity();
        transactionTextEntity.setTransactionTextPKEntity(transactionTextPKEntity);
        transactionTextEntity.setText(transactionTextDto.getText());
        transactionTextRepository.save(transactionTextEntity);
    }
}
