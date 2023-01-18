package za.co.raretag.mawabes.service;

import org.springframework.beans.factory.annotation.Autowired;
import za.co.raretag.mawabes.dao.TicketLogDao;
import za.co.raretag.mawabes.dto.*;
import za.co.raretag.mawabes.object.transaction.TransactionDTO;
import za.co.raretag.mawabes.utils.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class TicketLogService implements TicketLogDao {
    @Autowired
    TransactionService transactionService;
    @Autowired
    PartnerService partnerService;
    @Autowired
    DocumentLinkService documentLinkService;
    Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    Instant end;
    Instant start;
    @Override
    public String create(TicketLogDto ticketObj) {
        long t = date.getTimeInMillis();
        String ticketLogNo = null;
        start = Instant.now();

        OrderHeaderDto orderHeader = new OrderHeaderDto();
        orderHeader.setType(OrderType.TICKETLOG);
        ticketLogNo = transactionService.create(orderHeader);

        if (ticketLogNo != null) {
            OrderPartnerDto partner = new OrderPartnerDto();

            partner.setTransaction(ticketLogNo);
            partner.setPartner(ticketObj.getPartnerID());
            partner.setFunction(PartnerFunction.LOGGEDBY);
            transactionService.addPartner(partner);

            OrderDateDto orderDate = new OrderDateDto();
            orderDate.setTransaction(ticketLogNo);
            orderDate.setType(DateType.START_DATE);
            transactionService.addDate(orderDate);

            if (ticketObj.getTicketTaskID() != null) {
                TransactionLinkDto link = new TransactionLinkDto();
                link.setTransaction1(ticketObj.getTicketTaskID());
                link.setTransaction2(ticketLogNo);
                link.setType(OrderType.TICKETLOG);
                transactionService.addLink(link);
            }
        }
        return ticketLogNo;
    }

    @Override
    public boolean endDate(String ticketLogID) {
        long t = date.getTimeInMillis();
        boolean checkVal = false;
        OrderHeaderDto orderHeader = transactionService.getHeader(ticketLogID);
        OrderDateDto orderDate = new OrderDateDto();
        if (orderHeader != null) {
            if (orderHeader.getType().equals(OrderType.TICKETLOG)) {
                orderDate.setTransaction(ticketLogID);
                orderDate.setType(DateType.END_DATE);
                transactionService.addDate(orderDate);
                checkVal = true;
            }
        }
        return checkVal;
    }

    @Override
    public ArrayList<TicketLogDto> getLogByTicket(String ticketTaskID) {
        TicketLogDto ticketObj = new TicketLogDto();
        ArrayList<TicketLogDto> ticketList = new ArrayList();
        if (ticketTaskID != null) {
            ArrayList<TransactionLinkDto> transactionLinks = transactionService.getLink(ticketTaskID);
            if (!transactionLinks.isEmpty()) {
                for (TransactionLinkDto link : transactionLinks) {
                    if (link.getType().equals(OrderType.TICKETLOG)) {
                        TicketLogQueryDto ticketLogQueryObj = new TicketLogQueryDto();
                        ticketLogQueryObj.setTicketLogID(link.getTransaction2());
                        ticketLogQueryObj.setTicketTaskID(ticketTaskID);

                        TicketLogDto ticketLog = getLog(ticketLogQueryObj);
                        ticketList.add(ticketLog);
                    }
                }
            }
        }
        return ticketList;
    }

    @Override
    public boolean removeLogs(String ticketID) {
        boolean delete = false;
        if (ticketID != null) {
            ArrayList<TicketLogDto> ticketLogsByTicket = getLogByTicket(ticketID);
            if (!ticketLogsByTicket.isEmpty()) {
                for (TicketLogDto logs : ticketLogsByTicket) {
                    TicketLogQueryDto ticketObj = new TicketLogQueryDto();
                    ticketObj.setTicketLogID(logs.getTicketLogID());
                    ticketObj.setTicketTaskID(logs.getTicketTaskID());
                    delete = removeTicketLog(ticketObj);
                }
            }
        } else {
            ArrayList<TicketLogDto> ticketLogs = getAllLogs();
            if (!ticketLogs.isEmpty()) {
                for (TicketLogDto logs : ticketLogs) {
                    TicketLogQueryDto ticketObj = new TicketLogQueryDto();
                    ticketObj.setTicketLogID(logs.getTicketLogID());
                    ticketObj.setTicketID(logs.getTicketID());
                    delete = removeTicketLog(ticketObj);
                }
            }
        }
        return delete;
    }

    @Override
    public TicketLogDto getLog(TicketLogQueryDto ticketLogQueryObj) {
        TicketLogDto ticketObj = new TicketLogDto();
        if (ticketLogQueryObj.getTicketLogID() != null) {
            OrderHeaderDto orderHeader = transactionService.getHeader(ticketLogQueryObj.getTicketLogID());
            if (orderHeader != null) {
                if (orderHeader.getType().equals(OrderType.TICKETLOG)) {
                    if (ticketLogQueryObj.getTicketTaskID() == null) {
                        ArrayList<TransactionLinkDto> transactionLinks = transactionService.getLinkByTransaction2(ticketLogQueryObj.getTicketLogID());
                        if (!transactionLinks.isEmpty()) {
                            for (TransactionLinkDto transactionLink : transactionLinks) {
                                if (transactionLink.getType().equals(OrderType.TICKETLOG)) {
                                    ticketObj.setTicketTaskID(transactionLink.getTransaction1());
                                }
                            }
                        }
                    } else {
                        ticketObj.setTicketTaskID(ticketLogQueryObj.getTicketTaskID());
                    }

                    ticketObj.setTicketLogID(orderHeader.getId());
                    OrderDateDto dateObj = transactionService.getDate(orderHeader.getId(), DateType.START_DATE);
                    if (dateObj != null) {
                        ticketObj.setStartTime(dateObj.getValue());
                    }
                    OrderDateDto endDateObj = transactionService.getDate(orderHeader.getId(), DateType.END_DATE);

                    if (endDateObj != null) {
                        ticketObj.setEndTime(endDateObj.getValue());
                    }

                    ArrayList<NoteDto> notes = transactionService.getNotes(orderHeader.getId());
                    if (!notes.isEmpty()) {
                        if (notes.size() == 1) {
                            for (NoteDto note : notes) {
                                ticketObj.setNote(note);
                            }
                        } else {
                            ticketObj.setNotes(notes);
                        }
                    }

                    if (ticketObj.getStartTime() != null && ticketObj.getEndTime() != null) {
                        end = Instant.now();
                        Duration timeElapsed = Duration.between(Conversion.stringToDateTime(ticketObj.getStartTime()).toInstant(), Conversion.stringToDateTime(ticketObj.getEndTime()).toInstant());
                        long hours = timeElapsed.toMinutes() / 60;
                        long minutes = timeElapsed.toMinutes() % 60;
                        long seconds = (timeElapsed.getSeconds() - (minutes * 60) - (hours * 3600));
                        long duration = hours + minutes + seconds;
                        if (hours <= 23 && minutes <= 59 && seconds <= 59) {
                            ticketObj.setDuration(hours + ":" + minutes + ":" + seconds);
                        } else {
                            hours = 24;
                            minutes = minutes * 00;
                            seconds = seconds * 00;
                            ticketObj.setDuration(hours + ":" + minutes + ":" + seconds);
                        }
                    }

                    ArrayList<OrderPartnerDto> partners = transactionService.getPartners(orderHeader.getId());
                    for (OrderPartnerDto partner : partners) {
                        if (partner.getFunction().equals(PartnerFunction.LOGGEDBY)) {
                            ticketObj.setPartnerID(partner.getPartner());
                            PartnerDto prt = partnerService.get(partner.getPartner());
                            if (prt != null) {
                                PersonDto person = new PersonDto(prt);
                                ticketObj.setPartner(person);
                            }
                        }
                    }
                }
            }
        }

        return ticketObj;
    }

    @Override
    public boolean removeTicketLog(TicketLogQueryDto ticketObj) {
        boolean delete = false;
        if (ticketObj.getTicketLogID() != null) {

            TicketLogDto ticketLog = getLog(ticketObj);
            if (ticketLog != null) {
                OrderHeaderDto order = new OrderHeaderDto();

                order.setId(ticketLog.getTicketLogID());
                transactionService.removeOrderHearder(order);

                OrderPartnerDto partner = new OrderPartnerDto();
                partner.setTransaction(ticketLog.getTicketLogID());
                partner.setPartner(ticketLog.getPartnerID());
                partner.setFunction(PartnerFunction.LOGGEDBY);

                transactionService.removePartner(partner);

                OrderDateDto orderDate = new OrderDateDto();
                orderDate.setTransaction(ticketLog.getTicketLogID());
                orderDate.setType(DateType.START_DATE);
                transactionService.removeDate(orderDate);

                OrderDateDto orderDateCreated = new OrderDateDto();
                orderDateCreated.setTransaction(ticketLog.getTicketLogID());
                orderDateCreated.setType(DateType.CREATED);
                transactionService.removeDate(orderDateCreated);

                DocumentLinkDto link = new DocumentLinkDto();
                link.setParent(ticketLog.getTicketLogID());
                link.setChild(ticketLog.getTicketID());
                link.setType(OrderType.TICKETLOG);
                documentLinkService.remove(link);

                if (ticketLog.getNotes() != null) {

                    ArrayList<NoteDto> notes = ticketLog.getNotes();

                    transactionService.removeNotes(notes);

                }
                delete = true;
            }
        }
        return false;
    }

    @Override
    public ArrayList<TicketLogDto> getAllLogs() {
        ArrayList<TicketLogDto> tickets = new ArrayList<>();
        TransactionQueryDto transactionQuery = new TransactionQueryDto();
        transactionQuery.setType(OrderType.TICKETLOG);
        ArrayList<TransactionDTO> transactions = transactionService.search(transactionQuery);
        for (TransactionDTO transaction : transactions) {
            TicketLogQueryDto ticketLogQueryObj = new TicketLogQueryDto();
            ticketLogQueryObj.setTicketLogID(transaction.getId());
            TicketLogDto ticketLog = getLog(ticketLogQueryObj);
            tickets.add(ticketLog);
        }
        return tickets;
    }
}
