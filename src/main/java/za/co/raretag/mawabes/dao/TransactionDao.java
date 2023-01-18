package za.co.raretag.mawabes.dao;

import com.mysql.cj.protocol.Message;
import za.co.raretag.mawabes.dto.*;
import za.co.raretag.mawabes.entity.TransactionEntity;
import za.co.raretag.mawabes.object.transaction.TransactionDTO;

import java.util.ArrayList;

public interface TransactionDao {
    String create(TransactionEntity transactionEntity);
    String update(TransactionEntity transactionEntity);
    String delete(String id);
    TransactionEntity findById(String id);
    ArrayList<MessageDto> addDate(OrderDateDto od);
    OrderHeaderDto getHeader (String orderId);
    OrderDateDto getDate (String orderId, String dateType);
    ArrayList<OrderPartnerDto> getPartners (String transactionId);
    ArrayList<TransactionDTO> search (TransactionQueryDto transactionQueryDto);
    boolean editPartner(OrderPartnerDto orderPartnerDto);
    boolean edit(OrderHeaderDto orderHeaderDto);
    boolean addPartner(OrderPartnerDto partner);
    ArrayList<TransactionLinkDto> getLink(String transaction1);
    ArrayList<TransactionLinkDto> getLinkByTransaction2(String transaction2);
    ArrayList<NoteDto> getNotes(String transaction);
    boolean editDate(OrderDateDto date);
    String addNote(NoteDto note);
    boolean addNotes(ArrayList<NoteDto> nts, String transaction);
    ArrayList<OrderDateDto> getDates(String startDate, String endDate, String type);
    String create(OrderHeaderDto order);
    boolean addLink(TransactionLinkDto transactionLink);
    boolean removeOrderHearder(OrderHeaderDto order);
    boolean removePartner(OrderPartnerDto partner);
    ArrayList<Message> removeDate(OrderDateDto od);
    boolean removeLink(TransactionLinkDto transactionLink);
    boolean removeNotes(ArrayList<NoteDto> notes);
}
