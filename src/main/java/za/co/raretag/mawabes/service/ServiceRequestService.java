package za.co.raretag.mawabes.service;

import com.mysql.cj.protocol.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dto.*;
import za.co.raretag.mawabes.entity.TransactionEntity;
import za.co.raretag.mawabes.dao.TransactionDao;
import za.co.raretag.mawabes.object.transaction.TransactionDTO;
import za.co.raretag.mawabes.repository.TransactionRepository;

import java.util.ArrayList;

@Service
public class ServiceRequestService implements TransactionDao {
    @Autowired
    TransactionRepository transactionRepository;


    @Override
    public String create(TransactionEntity transactionEntity) {
        return null;
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
        return null;
    }

    @Override
    public OrderHeaderDto getHeader(String orderId) {
        return null;
    }

    @Override
    public OrderDateDto getDate(String orderId, String dateType) {
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

        return false;
    }
}
