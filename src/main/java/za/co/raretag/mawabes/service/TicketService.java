package za.co.raretag.mawabes.service;

import jakarta.persistence.RollbackException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.raretag.mawabes.dao.TicketDao;
import za.co.raretag.mawabes.dto.*;
import za.co.raretag.mawabes.entity.TransactionDateEntity;
import za.co.raretag.mawabes.entity.TransactionDatePKEntity;
import za.co.raretag.mawabes.entity.TransactionEntity;
import za.co.raretag.mawabes.object.transaction.TransactionDTO;
import za.co.raretag.mawabes.object.user.UserDTO;
import za.co.raretag.mawabes.repository.TicketRepository;
import za.co.raretag.mawabes.repository.TransactionDateRepository;
import za.co.raretag.mawabes.utils.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TicketService implements TicketDao {
    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    TransactionService transactionService;

    @Autowired
    TransactionDateRepository transactionDateRepository;

    @Autowired
    PartnerService partnerService;

    @Autowired
    TicketLogService ticketLogService;


//    public List<TransactionEntity> getAllTickets(){
//
//        List<TransactionEntity> tickets = new ArrayList<>();
//        ticketRepository.findAll().forEach(tickets1->tickets.add(tickets1));
//        return tickets;
//    }

    public TransactionEntity getTicketsById (String id){
        return ticketRepository.findById(id).get();
    }

    public void saveOrUpdate(TransactionEntity tickets){
        ticketRepository.save(tickets);
    }

    public void delete(String id){
        ticketRepository.deleteById(id);
    }

    public void update(TransactionEntity tickets, String id){
        ticketRepository.save(tickets);
    }

    @Override
    public String create(TicketDto ticketDto) {
        String ticketNr = null;
        OrderDateDto orderDateDto = new OrderDateDto();
        orderDateDto.setType(OrderType.TICKET);
        orderDateDto.setSubType(ticketDto.getCategory());
        orderDateDto.setDescription(ticketDto.getDescription());
        orderDateDto.setSubDescription(ticketDto.getSubDescription());
        orderDateDto.setStatus(Status.NEW);
        orderDateDto.setSubStatus(ticketDto.getPriority());

        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setType(OrderType.TICKET);
        transactionEntity.setSubType(ticketDto.getCategory());
        transactionEntity.setStatus(Status.NEW);
        transactionEntity.setSubStatus(ticketDto.getPriority());
        transactionEntity.setDescription(ticketDto.getDescription());
        transactionEntity.setSubDescription(ticketDto.getSubDescription());
        ticketNr = transactionService.create(transactionEntity);

        if(ticketNr != null){
            OrderPartnerDto client = new OrderPartnerDto();
            OrderPartnerDto loggedBy = new OrderPartnerDto();

            client.setTransaction(ticketNr);
            client.setPartner(ticketDto.getClientId());
            client.setFunction(PartnerFunction.CLIENT);
            //transactionService.addPartner(client);

            loggedBy.setTransaction(ticketNr);
            loggedBy.setPartner(ticketDto.getClientId());
            loggedBy.setFunction(PartnerFunction.LOGGEDBY);
            //transactionService.addPartner(loggedBy);

            Date dueDate;
            TransactionDatePKEntity txnDatePKEntity = new TransactionDatePKEntity();
            txnDatePKEntity.setTransaction(ticketNr);
            txnDatePKEntity.setType(DateType.DUE_DATE);
            TransactionDateEntity transactionDateEntity = new TransactionDateEntity();
            transactionDateEntity.setTransactionDatePK(txnDatePKEntity);

            if(ticketDto.getPriority().equals(PriorityType.CRITICAL)){
                dueDate = Conversion.addHoursToDate(new Date(), 2);
                transactionDateEntity.setValue(dueDate);
            }
            if(ticketDto.getPriority().equals(PriorityType.HIGH)){
                dueDate = Conversion.addHoursToDate(new Date(), 24);
                transactionDateEntity.setValue(dueDate);
            }
            if(ticketDto.getPriority().equals(PriorityType.MEDIUM)){
                dueDate = Conversion.addHoursToDate(new Date(), 48);
                transactionDateEntity.setValue(dueDate);
            }
            if(ticketDto.getPriority().equals(PriorityType.LOW)){
                dueDate = Conversion.addHoursToDate(new Date(), 72);
                transactionDateEntity.setValue(dueDate);
            }

            try{
                transactionDateRepository.save(transactionDateEntity);
            } catch (RollbackException ex){
                Logger.getLogger(TicketService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(TicketService.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Notifications
        }

        return null;
    }

    @Override
    public TransactionEntity findById(String id) {
        try {
            TransactionEntity transaction = ticketRepository.findById(id).get();
            return transaction;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public TicketDto getTicket(String ticket) {
        TicketDto tk = new TicketDto();
        OrderDateDto dueDate = new OrderDateDto();
        OrderDateDto dateLogged = new OrderDateDto();
        OrderHeaderDto orderHeaderDto = transactionService.getHeader(ticket);

        if(orderHeaderDto != null){
            if(orderHeaderDto.getType().equals(OrderType.TICKET)){
                tk.setId(orderHeaderDto.getId());
                //tk.setCategory(fieldService.getFieldOptionDescription("CATEGORY", orderHeaderDto.getSubType()));
                tk.setStatus(orderHeaderDto.getStatus());
                tk.setPriority(orderHeaderDto.getSubStatus());
                tk.setSubDescription(orderHeaderDto.getDescription());

                dueDate = transactionService.getDate(orderHeaderDto.getId(), DateType.DUE_DATE);
                if(dueDate != null){
                    tk.setDueDate(dueDate.getValue());
                }
                dateLogged = transactionService.getDate(orderHeaderDto.getId(), DateType.CREATED);
                if(dateLogged != null){
                    tk.setDateLogged(dateLogged.getValue());
                }
                OrderDateDto cancelDate = transactionService.getDate(orderHeaderDto.getId(), DateType.CANCEL_DATE);
                if(cancelDate != null){
                    tk.setCancelDate(cancelDate.getValue());
                }
                OrderDateDto resolveDate = transactionService.getDate(orderHeaderDto.getId(), DateType.RESOLVE_DATE);
                if(resolveDate != null){
                    tk.setResolveDate(resolveDate.getValue());
                }
                OrderDateDto completeDate = transactionService.getDate(orderHeaderDto.getId(), DateType.COMPLETED_DATE);
                if(completeDate != null){
                    tk.setResolveDate(completeDate.getValue());
                }

                ArrayList<OrderPartnerDto> partners = transactionService.getPartners(orderHeaderDto.getId());
                ArrayList<PersonDto> currentPartner = new ArrayList<>();
                for(OrderPartnerDto partner : partners){
                    if(partner.getFunction().equals(PartnerFunction.CLIENT)){
                        tk.setClientId(partner.getPartner());
                    }

                    PartnerDto prt = new PartnerDto();
                    if(partner.getFunction().equals(PartnerFunction.EMPLOYEE_RESPONSIBLE)){
                        prt = partnerService.get(partner.getPartner());
                        if(prt != null){
                            PersonDto person = new PersonDto();
                            currentPartner.add(person);
                            tk.setAssignedTo(currentPartner);
                        }
                    }

                    if(partner.getFunction().equals(PartnerFunction.LOGGEDBY)){
                        tk.setAssignedByID(partner.getPartner());
                        prt = partnerService.get(partner.getPartner());

                        if(prt != null){
                            PersonDto person = new PersonDto();
                            tk.setAssignedBy(person);
                        }
                    }
                }
            }
        }
        return tk;
    }

    @Override
    public ArrayList<TicketDto> getTickets(TicketDto ticket) {
        ArrayList<TicketDto> tickets = new ArrayList<>();

        if(ticket.getClientId() != null && ticket.getStatus() == null){
            TransactionQueryDto query = new TransactionQueryDto();
            query.setPartnerNo(ticket.getClientId());
            query.setPartnerFunction(PartnerFunction.CLIENT);
            ArrayList<TransactionDTO> transactions = transactionService.search(query);

            for(TransactionDTO transactionDTO : transactions){
                TicketDto tkt = new TicketDto();
                if(transactionDTO.getType().equals(OrderType.TICKET)){
                    tkt = getTicket(transactionDTO.getId());
                    tickets.add(tkt);
                }
            }
        }

        if(ticket.getAssignedToID() != null){
            TransactionQueryDto query = new TransactionQueryDto();
            query.setPartnerNo(ticket.getClientId());
            query.setPartnerFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
            ArrayList<TransactionDTO> transactions = transactionService.search(query);

            for(TransactionDTO transactionDTO : transactions){
                TicketDto tkt = new TicketDto();
                if(transactionDTO.getType().equals(OrderType.TICKET)){
                    tkt = getTicket(transactionDTO.getId());
                    tickets.add(tkt);
                }
            }
        }

        if(ticket.getClientId() != null && ticket.getStatus() == null){
            TransactionQueryDto query = new TransactionQueryDto();
            String status = ticket.getStatus().replaceAll(" ", "").toUpperCase();
            query.setPartnerNo(ticket.getClientId());
            query.setPartnerFunction(PartnerFunction.CLIENT);
            ArrayList<TransactionDTO> transactions = transactionService.search(query);

            for(TransactionDTO transactionDTO : transactions){
                TicketDto tkt = new TicketDto();
                if(transactionDTO.getType().equals(OrderType.TICKET)){
                    tkt = getTicket(transactionDTO.getId());
                    tickets.add(tkt);
                }
            }
        }
        return tickets;
    }

    @Override
    public boolean editTicket(TicketDto ticketDto) {
        boolean edited = false;

        if(ticketDto.getAssignedToID() != null){
            OrderPartnerDto assignTo = new OrderPartnerDto();
            assignTo.setFunction((PartnerFunction.EMPLOYEE_RESPONSIBLE));
            assignTo.setTransaction(ticketDto.getId());
            assignTo.setPartner(ticketDto.getAssignedToID());
            edited = transactionService.editPartner(assignTo);

            if(edited){
                OrderDateDto dateDto = transactionService.getDate(ticketDto.getId(), DateType.DUE_DATE);
                if(dateDto != null){
                    UserQueryDto queryParams = new UserQueryDto();
                    queryParams.setPartner(ticketDto.getAssignedToID());

                    //NOTIFICATION
                }
            }
        }
        return edited;
    }

    @Override
    public boolean openTicket(TicketDto ticketDto) {
        boolean progress = false;
        OrderPartnerDto assignTo = new OrderPartnerDto();
        boolean assignedPartner = false;
        boolean assignedOrg = false;
        OrderHeaderDto orderHeaderDto = transactionService.getHeader(ticketDto.getId());
        if(orderHeaderDto != null){
            if(orderHeaderDto.getType().equals(OrderType.TICKET)){
                ArrayList<OrderPartnerDto> partners = transactionService.getPartners(orderHeaderDto.getId());
                ArrayList<PersonDto> approvers = ticketDto.getAssignedTo();
                if (!partners.isEmpty()){
                    for(OrderPartnerDto partner : partners){
                        for (PersonDto assignedToID : approvers){
                            if(partner.getFunction().equals(PartnerFunction.EMPLOYEE_RESPONSIBLE)
                                    && partner.getPartner().equals(assignedToID.getId())){
                                orderHeaderDto.setStatus(Status.OPEN);
                                progress = transactionService.edit(orderHeaderDto);
                                OrderDateDto dateDto = transactionService.getDate(ticketDto.getId(), DateType.DUE_DATE);
                                assignedPartner = true;
                                if(dateDto != null){
                                    UserQueryDto queryParams = new UserQueryDto();
                                    queryParams.setPartner(assignedToID.getId());

                                    //NOTIFICATION
                                }
                            } else if (partner.getFunction().equals(PartnerFunction.ORGANIZATION_RESPONSIBLE)
                                    && partner.getPartner().equals(assignedToID.getId())){
                                orderHeaderDto.setStatus(Status.OPEN);
                                progress = transactionService.edit(orderHeaderDto);
                                OrderDateDto dateDto = transactionService.getDate(ticketDto.getId(), DateType.DUE_DATE);
                                assignedOrg = false;
                                if(dateDto != null){
                                    UserQueryDto queryParams = new UserQueryDto();
                                    queryParams.setPartner(assignedToID.getId());

                                    //NOTIFICATION
                                }
                            }
                        }
                    }
                }

                for(PersonDto assignedToID : approvers){
                    ArrayList<RelationDto> relationPartner = partnerService.getRelationByPartner1(assignedToID.getId());
                    if(!relationPartner.isEmpty()){
                        for(RelationDto partnerRelation : relationPartner){
                            if(partnerRelation.getPartner1().equals(assignedToID.getId()) && !assignedOrg){
                                assignTo.setTransaction(ticketDto.getId());
                                assignTo.setPartner((assignedToID.getId()));
                                assignTo.setFunction(PartnerFunction.ORGANIZATION_RESPONSIBLE);
                                progress = transactionService.addPartner(assignTo);

                                if(progress){
                                    OrderHeaderDto order = new OrderHeaderDto();
                                    order.setId(ticketDto.getId());
                                    order.setStatus(Status.OPEN);
                                    progress = transactionService.edit(order);

                                    OrderDateDto dateDto = transactionService.getDate(ticketDto.getId(), DateType.DUE_DATE);
                                    if(dateDto != null){
                                        UserQueryDto queryParams = new UserQueryDto();
                                        queryParams.setPartner(assignedToID.getId());

                                        //NOTIFICATION
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean cancel(TicketDto ticket) {
        boolean cancelled = false;
        ArrayList<TicketTaskDto> ticketTaskList = getticketTasks(ticket.getId());
        if(!ticketTaskList.isEmpty()){
            for(TicketTaskDto ticketTaskDto : ticketTaskList){
                if(ticketTaskDto.getStatus().toUpperCase().equals(Status.ACTIVE)){
                    OrderHeaderDto orderHeader = transactionService.getHeader(ticketTaskDto.getTicketTaskID());
                    orderHeader.setStatus(Status.CANCELLED);
                    OrderDateDto orderDate = transactionService.getDate(ticket.getId(), DateType.CANCEL_DATE);
                    if(orderDate != null){
                        OrderDateDto orderCancelDate = new OrderDateDto();
                        orderCancelDate.setTransaction(ticket.getId());
                        orderCancelDate.setType(DateType.CANCEL_DATE);
                        transactionService.editDate(orderCancelDate);
                    } else {
                        OrderDateDto orderCancelDate = new OrderDateDto();
                        orderCancelDate.setTransaction(ticket.getId());
                        orderCancelDate.setType(DateType.CANCEL_DATE);
                        transactionService.addDate(orderCancelDate);
                    }
                    cancelled = transactionService.edit(orderHeader);
                }

                if(ticketTaskDto.getStatus().toUpperCase().equals(Status.INPROGRESS)){
                    ArrayList<TicketLogDto> taskLogs = ticketTaskDto.getTaskLogs();
                    if(!taskLogs.isEmpty()){
                        for(TicketLogDto log : taskLogs){
                            OrderDateDto orderDate = transactionService.getDate(log.getTicketLogID(), DateType.END_DATE);
                            if(orderDate == null){
                                OrderHeaderDto orderHeader = transactionService.getHeader(ticketTaskDto.getTicketTaskID());
                                 ticketLogService.endDate(log.getTicketTaskLogID());
                                 orderHeader.setStatus(Status.CANCELLED);
                                 OrderDateDto orderDateCancel = transactionService.getDate(ticket.getId(), DateType.CANCEL_DATE);
                                 if(orderDateCancel != null){
                                     OrderDateDto orderCancelDate = new OrderDateDto();
                                     orderCancelDate.setTransaction(ticket.getId());
                                     orderCancelDate.setType(DateType.CANCEL_DATE);
                                     transactionService.editDate(orderDateCancel);
                                 } else {
                                     OrderDateDto orderCancelDate = new OrderDateDto();
                                     orderCancelDate.setTransaction(ticket.getId());
                                     orderCancelDate.setType(DateType.CANCEL_DATE);
                                     transactionService.addDate(orderCancelDate);
                                 }
                                 cancelled = transactionService.edit(orderHeader);
                            }
                        }
                    }
                }
            }
        } else {
            OrderHeaderDto orderHeader = transactionService.getHeader(ticket.getId());
            if(orderHeader != null){
                OrderDateDto orderDateCancel = transactionService.getDate(ticket.getId(), DateType.CANCEL_DATE);
                if(orderDateCancel != null){
                    OrderDateDto orderCancelDate = new OrderDateDto();
                    orderCancelDate.setTransaction(orderDateCancel.getTransaction());
                    orderCancelDate.setType(DateType.CANCEL_DATE);
                    transactionService.editDate(orderDateCancel);
                } else {
                    OrderDateDto orderCancelDate = new OrderDateDto();
                    orderCancelDate.setTransaction(ticket.getId());
                    orderCancelDate.setType(DateType.CANCEL_DATE);
                    transactionService.addDate(orderCancelDate);
                }
                orderHeader.setStatus(Status.CANCELLED);
                cancelled = transactionService.edit(orderHeader);
            }
        }
        return cancelled;
    }

    @Override
    public ArrayList<TicketTaskDto> getticketTasks(String ticket) {
        ArrayList<TicketTaskDto> ticketTaskList = new ArrayList<>();
        if(ticket != null){
            ArrayList<TransactionLinkDto> transactionLink = transactionService.getLink(ticket);
            if(!transactionLink.isEmpty()){
                for(TransactionLinkDto transactionLinkDto : transactionLink){
                    if(transactionLinkDto.getType().toUpperCase().equals(OrderType.TICKET_TASK)){
                        TicketTaskDto ticketTaskDto = getTicketTask(transactionLinkDto.getTransaction2());
                        if(ticketTaskDto.getTicketID() != null){
                            ticketTaskList.add(ticketTaskDto);
                        }
                    }
                }
            }
        }
        return ticketTaskList;
    }

    @Override
    public TicketTaskDto getTicketTask(String ticketTaskID) {
        TicketTaskDto ticketTaskDto = new TicketTaskDto();
    if(ticketTaskID != null){
        OrderHeaderDto orderHeaderDto = transactionService.getHeader(ticketTaskID);
        if(orderHeaderDto != null){
            if(orderHeaderDto.getType().equals(OrderType.TICKET_TASK));
            ticketTaskDto.setTicketTaskID(ticketTaskID);
            ticketTaskDto.setType(orderHeaderDto.getSubType());
            ticketTaskDto.setStatus(orderHeaderDto.getStatus());
            ticketTaskDto.setSummary(orderHeaderDto.getDescription());
            ticketTaskDto.setTaskDescription(orderHeaderDto.getSubDescription());

            ArrayList<OrderPartnerDto> orderPartnerList = transactionService.getPartners(ticketTaskID);
            if(!orderPartnerList.isEmpty()){
                for(OrderPartnerDto orderPartnerDto : orderPartnerList){
                    if(orderPartnerDto.getFunction().toUpperCase().equals(PartnerFunction.ASSIGNTO)){
                        ticketTaskDto.setAssignedTo(orderPartnerDto.getPartner());
                        PartnerDto partner = partnerService.get(ticketTaskDto.getAssignedTo());
                        if(partner != null){
                            PersonDto person = new PersonDto(partner);
                            ticketTaskDto.setPersonAssignTo(person);
                        }
                    }
                    if(orderPartnerDto.getFunction().toUpperCase().equals(PartnerFunction.ASSIGNBY)){
                        ticketTaskDto.setAssignedBy(orderPartnerDto.getPartner());
                        PartnerDto partnerAssignBy = partnerService.get(orderPartnerDto.getPartner());
                        if(partnerAssignBy != null){
                            PersonDto person = new PersonDto(partnerAssignBy);
                            ticketTaskDto.setPersonAssignedBy(person);
                        }
                    }
                }
            }

           ArrayList<TransactionLinkDto> transactionLink = transactionService.getLinkByTransaction2(ticketTaskID);
            if (!transactionLink.isEmpty()){
                for(TransactionLinkDto link : transactionLink){
                    if(link.getType().toUpperCase().equals(OrderType.TICKET_TASK)){
                        ticketTaskDto.setTicketID(link.getTransaction1());
                    }
                }
            }

            ArrayList<TicketLogDto> taskLogs = getLogByTicket(ticketTaskID);
            if(!taskLogs.isEmpty()){
                ticketTaskDto.setTaskLogs(taskLogs);

                int sumHours = 0;
                int sumMinutes = 0;
                int sumSeconds = 0;
                for (TicketLogDto log : taskLogs){
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:m:s");
                    if(log.getDuration() != null){
                        LocalTime time = LocalTime.parse(log.getDuration(), timeFormatter);
                        sumHours += time.getHour();
                        sumMinutes += time.getMinute();
                        sumSeconds += time.getSecond();
                    }
                }
                sumMinutes += sumSeconds / 60;
                sumSeconds %= 60;
                sumHours += sumMinutes / 60;
                sumMinutes %= 60;
                ticketTaskDto.setDuration(sumHours + ":" + sumMinutes + ":" + sumSeconds);
                OrderDateDto dueDate = transactionService.getDate(ticketTaskID, DateType.DUE_DATE);
                if(dueDate != null){
                    ticketTaskDto.setDueDate(dueDate.getValue());
                }
                ArrayList<Date> startDateList = new ArrayList<Date>();
                ArrayList<Date> endDateList = new ArrayList<Date>();
                if(!taskLogs.isEmpty()){
                    for(TicketLogDto date : taskLogs){
                        if(date.getStartTime() != null){
                            startDateList.add(Conversion.stringToDateTime(date.getStartTime()));
                        }
                        if(date.getEndTime() != null){
                            endDateList.add(Conversion.stringToDateTime(date.getEndTime()));
                        }
                    }
                }

                if(orderHeaderDto.getStatus().toUpperCase().equals(Status.COMPLETED)){
                    Date maximumDate = new Date();
                    if(!endDateList.isEmpty()){
                        maximumDate = Collections.max(endDateList);
                    }
                    if(maximumDate != null){
                        ticketTaskDto.setEndTime(Conversion.dateTimeToString(maximumDate));
                    }
                }

                Date minimumDate = new Date();
                if (!startDateList.isEmpty()) {
                    minimumDate = Collections.min(startDateList);
                }
                if(minimumDate != null){
                    ticketTaskDto.setStartTime(Conversion.dateTimeToString(minimumDate));
                }

                ArrayList<NoteDto> notes = transactionService.getNotes(ticketTaskID);
                if(!notes.isEmpty()){
                    ticketTaskDto.setNotes(notes);
                }
            }
        }
    }
        return ticketTaskDto;
    }

    @Override
    public ArrayList<TicketLogDto> getLogByTicket(String ticketTaskID) {
        return null;
    }

    @Override
    public boolean reject(TicketDto ticket, NoteDto note) {
        OrderHeaderDto order = new OrderHeaderDto();
        order.setId(ticket.getId());
        order.setStatus(Status.INPROGRESS);
        boolean rejected = transactionService.edit(order);

        if(rejected){
            TicketDto ticketLog = new TicketDto();
            ticketLog.setId(ticket.getId());
            note.setType("RESOLUTIONREJECT");
            transactionService.addNote(note);
        }
        return rejected;
    }

    @Override
    public boolean awaitingCustomer(TicketDto ticket, NoteDto note) {
        boolean resolve = true;
        boolean awaiting = false;
        ArrayList<TicketTaskDto> ticketTaskList = getticketTasks(ticket.getId());
        if (!ticketTaskList.isEmpty()) {
            for (TicketTaskDto ticketTask : ticketTaskList) {
                if (ticketTask.getStatus().equals(Status.ACTIVE)) {
                    resolve = false;
                }
            }
        }
        if (resolve) {
            OrderHeaderDto order = new OrderHeaderDto();
            order.setId(ticket.getId());
            order.setStatus(Status.RESOLVED);
            awaiting = transactionService.edit(order);
            OrderDateDto orderDate = transactionService.getDate(ticket.getId(), DateType.RESOLVE_DATE);
            if (orderDate != null) {
                OrderDateDto orderResolveDate = new OrderDateDto();
                orderResolveDate.setTransaction(ticket.getId());
                orderResolveDate.setType(DateType.RESOLVE_DATE);
                transactionService.editDate(orderResolveDate);
            }else {
                OrderDateDto orderResolveDate = new OrderDateDto();
                orderResolveDate.setTransaction(ticket.getId());
                orderResolveDate.setType(DateType.RESOLVE_DATE);
                transactionService.addDate(orderResolveDate);
            }

            if (awaiting) {
                TicketDto ticketLog = new TicketDto();
                ticketLog.setId(ticket.getId());
                note.setType("TICKETRESOLUTION");
                transactionService.addNote(note);
            }
        }
        return awaiting;
    }

    @Override
    public ArrayList<TicketDto> getAllTickets() {
        ArrayList<TicketDto> tickets = new ArrayList<>();
        TransactionQueryDto transactionQuery = new TransactionQueryDto();
        transactionQuery.setType(OrderType.TICKET);

        ArrayList<TransactionDTO> transactions = transactionService.search(transactionQuery);
        for (TransactionDTO transaction : transactions) {
            TicketDto tkt = new TicketDto();
            tkt = getTicket(transaction.getId());
            tickets.add(tkt);
        }
        return tickets;
    }

    @Override
    public boolean inprogress(TicketDto ticket) {
        boolean awaiting = false;
        OrderHeaderDto order = new OrderHeaderDto();
        order.setId(ticket.getId());
        order.setStatus(Status.INPROGRESS);
        awaiting = transactionService.edit(order);
        return awaiting;
    }

    @Override
    public boolean resloved(TicketDto ticket) {
        boolean resloved = false;
        OrderHeaderDto order = new OrderHeaderDto();
        order.setId(ticket.getId());
        order.setStatus(Status.COMPLETED);
        resloved = transactionService.edit(order);

        if (resloved) {
            OrderDateDto orderDate = transactionService.getDate(ticket.getId(), DateType.COMPLETED_DATE);
            if (orderDate != null) {
                OrderDateDto orderCompleteDate = new OrderDateDto();
                orderCompleteDate.setTransaction(ticket.getId());
                orderCompleteDate.setType(DateType.COMPLETED_DATE);
                transactionService.editDate(orderCompleteDate);
            } else {
                OrderDateDto orderCompleteDate = new OrderDateDto();

                orderCompleteDate.setTransaction(ticket.getId());
                orderCompleteDate.setType(DateType.COMPLETED_DATE);
                transactionService.addDate(orderCompleteDate);
            }
        }
        return resloved;
    }

    @Override
    public ArrayList<TicketDto> getTicketsByDates(String startDate, String endDate, String type, String status) {
        ArrayList<TicketDto> ticketList = new ArrayList<>();
        if (type != null) {
            ArrayList<OrderDateDto> transactionDates = transactionService.getDates(startDate, endDate, type);
            if (!transactionDates.isEmpty()) {
                for (OrderDateDto orderDateList : transactionDates) {
                    TicketDto ticket = getTicket(orderDateList.getTransaction());
                    if (ticket != null) {
                        if (status == null) {
                            ticketList.add(ticket);
                        } else {
                            if (ticket.getStatus().equals(status)) {
                                ticketList.add(ticket);
                            }
                        }
                    }
                }
            }
        }else {
            ArrayList<OrderDateDto> transactionDates = transactionService.getDates(startDate, endDate, DateType.CREATED);
            if (!transactionDates.isEmpty()) {
                for (OrderDateDto orderDateList : transactionDates) {
                    TicketDto ticket = getTicket(orderDateList.getTransaction());
                    if (ticket != null) {
                        if (status == null) {
                            ticketList.add(ticket);
                        } else {
                            if (ticket.getStatus().equals(status)) {
                                ticketList.add(ticket);
                            }
                        }
                    }
                }
            }
        }
        return ticketList;
    }

    @Override
    public TicketDto getTicketwithDuration(String ticket) {
        TicketDto tk = new TicketDto();
        OrderDateDto dateDue = new OrderDateDto();
        OrderDateDto dateLogged = new OrderDateDto();
        OrderHeaderDto orderHeader = transactionService.getHeader(ticket);

        if (orderHeader != null) {
            if (orderHeader.getType().equals(OrderType.TICKET)) {
                tk.setId(orderHeader.getId());
                //tk.setCategory(fieldBean.getFieldOptionDescription("CATEGORY", orderHeader.getSubType()));
                tk.setStatus(orderHeader.getStatus());
                tk.setPriority(orderHeader.getSubStatus());
                tk.setDescription(orderHeader.getDescription());
                tk.setSubDescription(orderHeader.getSubDescription());

                dateDue = transactionService.getDate(orderHeader.getId(), DateType.DUE_DATE);
                if (dateDue != null) {
                    tk.setDueDate(dateDue.getValue());
                }
                dateLogged = transactionService.getDate(orderHeader.getId(), DateType.CREATED);
                if (dateLogged != null) {
                    tk.setDateLogged(dateLogged.getValue());
                }
                OrderDateDto cancelDate = transactionService.getDate(orderHeader.getId(), DateType.CANCEL_DATE);
                if (cancelDate != null) {
                    tk.setCancelDate(cancelDate.getValue());
                }
                OrderDateDto resolveDate = transactionService.getDate(orderHeader.getId(), DateType.RESOLVE_DATE);
                if (resolveDate != null) {
                    tk.setResolveDate(resolveDate.getValue());
                }
                OrderDateDto orderCompleteDate = transactionService.getDate(orderHeader.getId(), DateType.COMPLETED_DATE);
                if (orderCompleteDate != null) {
                    tk.setCompletedDate(orderCompleteDate.getValue());
                }

                ArrayList<OrderPartnerDto> partner = transactionService.getPartners(orderHeader.getId());
                ArrayList<PersonDto> currentPartner = new ArrayList<>();
                for (OrderPartnerDto partners : partner) {
                    if (partners.getFunction().equals(PartnerFunction.CLIENT)) {
                        tk.setClientId(partners.getPartner());
                    }
                    PartnerDto prt = new PartnerDto();
                    if (partners.getFunction().equals(PartnerFunction.EMPLOYEE_RESPONSIBLE)) {
                        prt = partnerService.get(partners.getPartner());
                        if (prt != null) {
                            PersonDto person = new PersonDto(prt);
                            currentPartner.add(person);
                            tk.setAssignedTo(currentPartner);
                        }
                    }

                    if (partners.getFunction().equals(PartnerFunction.LOGGEDBY)) {
                        tk.setAssignedByID(partners.getPartner());
                        prt = partnerService.get(partners.getPartner());
                        if (prt != null) {
                            PersonDto person = new PersonDto(prt);
                            tk.setAssignedBy(person);
                        }
                    }
                }

                ArrayList<TicketTaskDto> ticketTask = getticketTasks(orderHeader.getId());
                int sumHours = 0;
                int sumMinutes = 0;
                int sumSeconds = 0;
                if (!ticketTask.isEmpty()) {
                    for (TicketTaskDto task : ticketTask) {
                        if (task.getStatus() != null) {
                            if (!task.getStatus().toUpperCase().equals(Status.CANCELLED)) {
                                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:m:s");
                                if (task.getDuration() != null) {
                                    LocalTime time = LocalTime.parse(task.getDuration(), timeFormatter);
                                    sumHours += time.getHour();
                                    sumMinutes += time.getMinute();
                                    sumSeconds += time.getSecond();
                                }
                            }
                        }
                    }
                }

                sumMinutes += sumSeconds / 60;
                sumSeconds %= 60;
                sumHours += sumMinutes / 60;
                sumMinutes %= 60;
                String strSum = String.format("%d hours : %d mins : %d sec", sumHours, sumMinutes, sumSeconds);
                tk.setDuration(strSum);
            }
        }
        return tk;
    }

    @Override
    public String createTask(TicketTaskDto ticketTaskObj) {
        String ticketTaskID = null;
        if (ticketTaskObj.getTicketID() != null && ticketTaskObj.getAssignedTo() != null) {
            TicketDto ticket = getTicket(ticketTaskObj.getTicketID());
            if (ticket != null) {
                OrderHeaderDto orderHeader = new OrderHeaderDto();
                orderHeader.setType(OrderType.TICKET_TASK);
                orderHeader.setSubType(ticket.getCategory());

                if (ticketTaskObj.getSummary() != null) {
                    orderHeader.setDescription(ticketTaskObj.getSummary());
                } else {
                    orderHeader.setDescription(ticket.getDescription());
                }

                if (ticketTaskObj.getTaskDescription() != null) {
                    orderHeader.setSubDescription(ticketTaskObj.getTaskDescription());
                } else {
                    orderHeader.setSubDescription(ticket.getSubDescription());
                }

                orderHeader.setStatus(Status.ACTIVE);
                orderHeader.setSubStatus(ticket.getPriority());
                ticketTaskID = transactionService.create(orderHeader);

                if (ticketTaskObj.getAssignedTo() != null) {
                    OrderPartnerDto partner = new OrderPartnerDto();
                    partner.setTransaction(ticketTaskID);
                    partner.setPartner(ticketTaskObj.getAssignedTo());
                    partner.setFunction(PartnerFunction.ASSIGNTO);
                    transactionService.addPartner(partner);
                }

                if (ticketTaskObj.getAssignedBy() != null) {
                    OrderPartnerDto partner = new OrderPartnerDto();
                    partner.setTransaction(ticketTaskID);
                    partner.setPartner(ticketTaskObj.getAssignedBy());
                    partner.setFunction(PartnerFunction.ASSIGNBY);
                    transactionService.addPartner(partner);
                }

                OrderDateDto orderDueDate = new OrderDateDto();
                if (ticketTaskObj.getDueDate() != null) {
                    orderDueDate.setTransaction(ticketTaskID);
                    orderDueDate.setType(DateType.DUE_DATE);
                    orderDueDate.setValue(ticketTaskObj.getDueDate());

                } else {
                    orderDueDate.setTransaction(ticketTaskID);
                    orderDueDate.setType(DateType.DUE_DATE);
                    orderDueDate.setValue(ticket.getDueDate());
                }

                transactionService.addDate(orderDueDate);

                TransactionLinkDto link = new TransactionLinkDto();
                link.setTransaction1(ticketTaskObj.getTicketID());
                link.setTransaction2(ticketTaskID);
                link.setType(OrderType.TICKET_TASK);
                transactionService.addLink(link);
            }
        }
        return ticketTaskID;
    }

    @Override
    public boolean taskEndTime(TicketTaskDto ticketTaskObj) {
        boolean checkVal = false;
        if (ticketTaskObj.getNote() != null) {
            OrderHeaderDto orderHeader = transactionService.getHeader(ticketTaskObj.getTicketTaskID());
            if (orderHeader != null) {
                ArrayList<TicketLogDto> tasksLogs = ticketLogService.getLogByTicket(orderHeader.getId());
                if (!tasksLogs.isEmpty()) {
                    for (TicketLogDto log : tasksLogs) {
                        if (log.getDuration() == null) {
                            ticketTaskObj.setTaskLogID(log.getTicketLogID());
                            break;
                        }
                    }
                }
                if (ticketTaskObj.getTaskLogID() != null) {
                    if (orderHeader.getType().toUpperCase().equals(OrderType.TICKET_TASK)) {
                        if (orderHeader.getStatus().toUpperCase().equals(Status.INPROGRESS)) {
                            orderHeader.setStatus(Status.ACTIVE);
                            ticketLogService.endDate(ticketTaskObj.getTaskLogID());
                            transactionService.edit(orderHeader);
                            ArrayList<NoteDto> notes = new ArrayList<>();
                            notes.add(ticketTaskObj.getNote());
                            transactionService.addNotes(notes, ticketTaskObj.getTaskLogID());
                            checkVal = true;
                        }
                    }
                }
            }
        }
        return checkVal;
    }

    @Override
    public boolean removeTicketTask(String ticketTaskID) {
        boolean remove = false;
        if (ticketTaskID != null) {
            TicketTaskDto taskRemove = getTicketTask(ticketTaskID);
            if (taskRemove != null) {
                OrderHeaderDto order = new OrderHeaderDto();
                order.setId(taskRemove.getTicketTaskID());
                transactionService.removeOrderHearder(order);

                OrderPartnerDto partner = new OrderPartnerDto();
                partner.setTransaction(taskRemove.getTicketTaskID());
                partner.setPartner(taskRemove.getAssignedTo());
                partner.setFunction(PartnerFunction.ASSIGNTO);
                transactionService.removePartner(partner);

                OrderDateDto orderDateCreated = new OrderDateDto();
                orderDateCreated.setTransaction(taskRemove.getTicketTaskID());
                orderDateCreated.setType(DateType.CREATED);
                transactionService.removeDate(orderDateCreated);

                TransactionLinkDto transactionLink = new TransactionLinkDto();
                transactionLink.setTransaction1(taskRemove.getTicketID());
                transactionLink.setTransaction2(ticketTaskID);
                transactionLink.setType(OrderType.TICKET_TASK);
                transactionService.removeLink(transactionLink);
                remove = true;

                if (remove) {
                    ticketLogService.removeLogs(ticketTaskID);
                }
            }
        }
        return remove;
    }

    @Override
    public String taskStartTime(TicketTaskDto ticketTaskObj) {
        String taskLogID = null;
        OrderHeaderDto orderHeader = transactionService.getHeader(ticketTaskObj.getTicketTaskID());
        if (orderHeader != null) {
            if (orderHeader.getType().equals(OrderType.TICKET_TASK)) {
                if (orderHeader.getStatus().toUpperCase().equals(Status.ACTIVE)) {
                    orderHeader.setStatus(Status.INPROGRESS);
                    TicketLogDto ticketLog = new TicketLogDto();
                    if (ticketTaskObj.getTaskLogLoggedBy() != null) {
                        ticketLog.setPartnerID(ticketTaskObj.getTaskLogLoggedBy());
                    } else {
                        ArrayList<OrderPartnerDto> orderPartnerList = transactionService.getPartners(ticketTaskObj.getTicketTaskID());
                        if (!orderPartnerList.isEmpty()) {
                            for (OrderPartnerDto orderPartner : orderPartnerList) {
                                if (orderPartner.getFunction().equals(PartnerFunction.ASSIGNTO)) {
                                    ticketLog.setPartnerID(orderPartner.getPartner());
                                }
                            }
                        }
                    }

                    ticketLog.setTicketTaskID(ticketTaskObj.getTicketTaskID());
                    transactionService.edit(orderHeader);
                    taskLogID = ticketLogService.create(ticketLog);
                }
            }
        }
        return taskLogID;
    }

    @Override
    public ArrayList<TicketTaskDto> getTaskAssigned(String assignedToID) {
        ArrayList<TicketTaskDto> ticketTasks = new ArrayList<>();
        TransactionQueryDto query = new TransactionQueryDto();
        query.setPartnerFunction(PartnerFunction.ASSIGNTO);
        query.setPartnerNo(assignedToID);
        ArrayList<TransactionDTO> transactionPartner = transactionService.search(query);

        if (!transactionPartner.isEmpty()) {
            for (TransactionDTO transactionObj : transactionPartner) {
                OrderHeaderDto orderHeader = transactionService.getHeader(transactionObj.getId());
                if (orderHeader != null) {
                    if (orderHeader.getType().equals(OrderType.TICKET_TASK)) {
                        TicketTaskDto ticketTaskObj = getTicketTask(orderHeader.getId());
                        if (ticketTaskObj != null) {
                            ticketTasks.add(ticketTaskObj);
                        }
                    }
                }
            }
        }
        return ticketTasks;
    }

    @Override
    public boolean editTask(TicketTaskDto ticketTaskObj) {
        boolean edit = false;
        if (ticketTaskObj != null) {
            OrderHeaderDto order = transactionService.getHeader(ticketTaskObj.getTicketTaskID());
            if (order != null) {
                if (order.getStatus().toUpperCase().equals(Status.ACTIVE) || order.getStatus().toUpperCase().equals(Status.INPROGRESS)) {
                    if (ticketTaskObj.getSummary() != null) {
                        order.setDescription(ticketTaskObj.getSummary());
                    }
                    if (ticketTaskObj.getTaskDescription() != null) {
                        order.setSubDescription(ticketTaskObj.getTaskDescription());
                    }
                    transactionService.edit(order);

                    if (ticketTaskObj.getAssignedTo() != null) {
                        ArrayList<OrderPartnerDto> orderPartnerList = transactionService.getPartners(ticketTaskObj.getTicketTaskID());
                        if (!orderPartnerList.isEmpty()) {
                            for (OrderPartnerDto orderPartner : orderPartnerList) {
                                if (orderPartner.getFunction().toUpperCase().equals(PartnerFunction.ASSIGNTO)) {
                                    orderPartner.setTransaction(ticketTaskObj.getTicketTaskID());
                                    transactionService.removePartner(orderPartner);
                                    orderPartner.setPartner(ticketTaskObj.getAssignedTo());
                                    transactionService.addPartner(orderPartner);
                                }
                            }
                        }
                    }
                    if (ticketTaskObj.getDueDate() != null) {
                        OrderDateDto dueDateObj = transactionService.getDate(ticketTaskObj.getTicketTaskID(), DateType.DUE_DATE);
                        if (dueDateObj != null) {
                            dueDateObj.setValue(ticketTaskObj.getDueDate());
                            transactionService.editDate(dueDateObj);
                        } else {
                            dueDateObj = new OrderDateDto();
                            dueDateObj.setTransaction(ticketTaskObj.getTicketTaskID());
                            dueDateObj.setType(DateType.DUE_DATE);
                            dueDateObj.setValue(ticketTaskObj.getDueDate());
                            transactionService.addDate(dueDateObj);
                        }
                    }
                    edit = true;
                }
            }
        }
        return edit;
    }

    @Override
    public boolean markTaskComplete(String ticketTaskID) {
        boolean complete = false;
        if (ticketTaskID != null) {
            OrderHeaderDto order = transactionService.getHeader(ticketTaskID);
            if (order != null) {
                TicketTaskDto ticketTaskObj = getTicketTask(ticketTaskID);
                if (ticketTaskObj != null) {
                    ArrayList<TicketLogDto> taskLogs = ticketTaskObj.getTaskLogs();
                    if (!taskLogs.isEmpty()) {
                        if (order.getStatus().toUpperCase().equals(Status.ACTIVE)) {
                            order.setStatus(Status.COMPLETED);
                            transactionService.edit(order);
                            complete = true;
                        }
                    }
                }
            }
        }
        return complete;
    }

    @Override
    public boolean cancelTask(TicketTaskDto ticketTaskObj) {
        boolean cancel = false;
        if (ticketTaskObj.getNote() != null) {

            if (ticketTaskObj.getTicketTaskID() != null) {
                OrderHeaderDto order = transactionService.getHeader(ticketTaskObj.getTicketTaskID());
                if (order != null) {
                    if (order.getStatus().toUpperCase().equals(Status.ACTIVE)) {
                        order.setStatus(Status.CANCELLED);
                        ArrayList<NoteDto> notes = new ArrayList<>();
                        notes.add(ticketTaskObj.getNote());
                        transactionService.addNotes(notes, ticketTaskObj.getTicketTaskID());
                        transactionService.edit(order);
                        cancel = true;
                    }
                }
            }
        }
        return cancel;
    }

    @Override
    public ArrayList<TicketDto> getTicketsByCategory(String category) {
        ArrayList<TicketDto> tickets = new ArrayList<>();
        TransactionQueryDto transactionQuery = new TransactionQueryDto();
        transactionQuery.setType(OrderType.TICKET);
        transactionQuery.setSubtype(category);

        ArrayList<TransactionDTO> transactions = transactionService.search(transactionQuery);
        for (TransactionDTO transaction : transactions) {
            TicketDto tkt = new TicketDto();
            tkt = getTicket(transaction.getId());
            if (tkt.getCategory() != null) {
                if (tkt.getCategory().replaceAll(" ", "").toUpperCase().equals(category)) {
                    tickets.add(tkt);
                }
            }
        }
        return tickets;
    }

    @Override
    public ArrayList<TicketDto> getTicketByStatus(String assignedToID, String status) {
        ArrayList<TicketDto> tickets = new ArrayList<>();
        TransactionQueryDto queryPartner = new TransactionQueryDto();
        queryPartner.setPartnerFunction(PartnerFunction.EMPLOYEE_RESPONSIBLE);
        queryPartner.setPartnerNo(assignedToID);
        ArrayList<TransactionDTO> transactionPartner = transactionService.search(queryPartner);

        TransactionQueryDto queryOrg = new TransactionQueryDto();
        queryOrg.setPartnerFunction(PartnerFunction.ORGANIZATION_RESPONSIBLE);
        queryOrg.setPartnerNo(assignedToID);
        ArrayList<TransactionDTO> transactionOrg = transactionService.search(queryOrg);

        if (!transactionPartner.isEmpty()) {
            for (TransactionDTO transactionObj : transactionPartner) {
                OrderHeaderDto orderHeader = transactionService.getHeader(transactionObj.getId());
                if (orderHeader != null) {
                    if (orderHeader.getType().equals(OrderType.TICKET) && orderHeader.getStatus().replaceAll(" ", "").toUpperCase().equals(status)) {
                        TicketDto ticketObj = getTicket(orderHeader.getId());
                        if (ticketObj != null) {
                            tickets.add(ticketObj);
                        }
                    }
                }
            }
        } else if (!transactionOrg.isEmpty()) {
            for (TransactionDTO transactionObj : transactionOrg) {
                OrderHeaderDto orderHeader = transactionService.getHeader(transactionObj.getId());
                if (orderHeader != null) {
                    if (orderHeader.getType().equals(OrderType.TICKET) && orderHeader.getStatus().replaceAll(" ", "").toUpperCase().equals(status)) {
                        TicketDto ticketObj = getTicket(orderHeader.getId());
                        if (ticketObj != null) {
                            tickets.add(ticketObj);
                        }
                    }
                }
            }
        }
        return tickets;
    }
}
