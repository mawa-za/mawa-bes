package za.co.raretag.mawabes.service;

import com.mysql.cj.protocol.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dao.TransactionDao;
import za.co.raretag.mawabes.dto.*;
import za.co.raretag.mawabes.entity.NumberRangeEntity;
import za.co.raretag.mawabes.entity.TransactionDateEntity;
import za.co.raretag.mawabes.entity.TransactionDatePKEntity;
import za.co.raretag.mawabes.entity.TransactionEntity;
import za.co.raretag.mawabes.object.transaction.TransactionDTO;
import za.co.raretag.mawabes.repository.NoteRepository;
import za.co.raretag.mawabes.repository.TransactionDateRepository;
import za.co.raretag.mawabes.repository.TransactionRepository;
import za.co.raretag.mawabes.utils.Constant;
import za.co.raretag.mawabes.utils.Conversion;
import za.co.raretag.mawabes.utils.DateType;
import za.co.raretag.mawabes.utils.Status;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

@Service
public class TransactionService implements TransactionDao {

    @Autowired
    NumberRangeService numberRange;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    TransactionDateRepository transactionDateRepository;
    @Autowired
    NoteRepository noteRepository;

    @Override
    public String create(TransactionEntity transactionEntity) {

        String txnNo = null;
        try {
            txnNo = numberRange.generateNumber(transactionEntity.getType());
            transactionEntity.setId(txnNo);
            if (transactionEntity.getStatus() == null) {
                transactionEntity.setStatus(Status.NEW);
            }
            transactionEntity.setValidFrom(new Date());
            transactionEntity.setValidTo(Conversion.stringToDate(Constant.END_DATE));
            transactionRepository.save(transactionEntity);
            OrderDateDto creationDate = new OrderDateDto();
            creationDate.setTransaction(txnNo);
            creationDate.setType(DateType.CREATED);
            addDate(creationDate);
        } catch (Exception ex) {
            return null;
        }

        return txnNo;
    }

    @Override
    public String update(TransactionEntity transactionEntity) {
        return null;
    }

    @Override
    public String delete(String id) {
        return null;
    }

    @Override
    public TransactionEntity findById(String id) {
        return null;
    }

    @Override
    public ArrayList<MessageDto> addDate(OrderDateDto od) {

        TransactionDatePKEntity txnDatePK = new TransactionDatePKEntity();
        txnDatePK.setTransaction(od.getTransaction());
        txnDatePK.setType(od.getType());
        TransactionDateEntity txnDate = new TransactionDateEntity();
        txnDate.setTransactionDatePK(txnDatePK);
        if (od.getValue() != null) {
            txnDate.setValue(Conversion.stringToDate(od.getValue()));
        } else {
            txnDate.setValue(new Date());
        }
        try {
            transactionDateRepository.save(txnDate);
        } catch (Exception ex) {
            return null;
        }

        return null;
    }

    @Override
    public OrderHeaderDto getHeader(String orderId) {
        return null;
    }

    @Override
    public OrderDateDto getDate(String orderId, String dateType) {
//        OrderDateDto orderDate = null;
//        TransactionDatePKEntity transactionDatePK = new TransactionDatePKEntity();
//        transactionDatePK.setTransaction(orderId);
//        transactionDatePK.setType(dateType);
//        Optional<TransactionDateEntity> transactionDate = transactionDateRepository.findById(transactionDatePK);
//        if (transactionDate != null) {
//            orderDate = new OrderDateDto();
//            orderDate.setTransaction(transactionDate.getTransactionDatePK().getTransaction());
//            orderDate.setType(transactionDate.getTransactionDatePK().getType());
//            orderDate.setValue(Conversion.dateTimeToString(transactionDate.getValue()));
//        }
//        return orderDate;
        return null;
    }

    @Override
    public ArrayList<OrderPartnerDto> getPartners(String transactionId) {
        return null;
    }

    @Override
    public ArrayList<TransactionDTO> search(TransactionQueryDto transactionQueryDto) {
        return null;
    }

    @Override
    public boolean editPartner(OrderPartnerDto orderPartnerDto) {
        return false;
    }

    @Override
    public boolean edit(OrderHeaderDto orderHeaderDto) {
        return false;
    }

    @Override
    public boolean addPartner(OrderPartnerDto partner) {
        return false;
    }

    @Override
    public ArrayList<TransactionLinkDto> getLink(String transaction1) {
        return null;
    }

    @Override
    public ArrayList<TransactionLinkDto> getLinkByTransaction2(String transaction2) {
        return null;
    }

    @Override
    public ArrayList<NoteDto> getNotes(String transaction) {
        return null;
    }

    @Override
    public boolean editDate(OrderDateDto date) {
        return false;
    }

    @Override
    public String addNote(NoteDto note) {
        return null;
    }

    @Override
    public boolean addNotes(ArrayList<NoteDto> nts, String transaction) {
        return false;
    }

    @Override
    public ArrayList<OrderDateDto> getDates(String startDate, String endDate, String type) {
        return null;
    }

    @Override
    public String create(OrderHeaderDto order) {
        return null;
    }

    @Override
    public boolean addLink(TransactionLinkDto transactionLink) {
        return false;
    }

    @Override
    public boolean removeOrderHearder(OrderHeaderDto order) {
        return false;
    }

    @Override
    public boolean removePartner(OrderPartnerDto partner) {
        return false;
    }

    @Override
    public ArrayList<Message> removeDate(OrderDateDto od) {
        return null;
    }

    @Override
    public boolean removeLink(TransactionLinkDto transactionLink) {
        return false;
    }

    @Override
    public boolean removeNotes(ArrayList<NoteDto> notes) {
        boolean delete = false;
        ArrayList<NoteDto> lists = notes;
        if (!lists.isEmpty()) {
            for (NoteDto note : lists) {
                try{
                    noteRepository.deleteById(note.getId());
                    delete = true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return delete;
    }
}
