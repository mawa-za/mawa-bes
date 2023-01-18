package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.dto.NoteDto;
import za.co.raretag.mawabes.dto.TicketDto;
import za.co.raretag.mawabes.dto.TicketLogDto;
import za.co.raretag.mawabes.dto.TicketTaskDto;
import za.co.raretag.mawabes.entity.TransactionEntity;

import java.util.ArrayList;

public interface TicketDao {
    String create(TicketDto ticketDto);
    TransactionEntity findById(String id);
    TicketDto getTicket(String ticket);
    ArrayList<TicketDto> getTickets (TicketDto ticket);
    boolean editTicket(TicketDto ticketDto);
    boolean openTicket(TicketDto ticketDto);
    boolean cancel (TicketDto ticket);
    ArrayList<TicketTaskDto> getticketTasks (String ticket);
    TicketTaskDto getTicketTask(String ticketTaskID);
    ArrayList<TicketLogDto> getLogByTicket(String ticketTaskID);
    boolean reject(TicketDto ticket, NoteDto note);
    boolean awaitingCustomer(TicketDto ticket, NoteDto note);
    ArrayList<TicketDto> getAllTickets();
    boolean inprogress(TicketDto ticket);
    boolean resloved(TicketDto ticket);
    ArrayList<TicketDto> getTicketsByDates(String startDate, String endDate, String type, String status);
    TicketDto getTicketwithDuration(String ticket);
    String createTask(TicketTaskDto ticketTaskObj);
    boolean taskEndTime(TicketTaskDto ticketTaskObj);
    boolean removeTicketTask(String ticketTaskID);
    String taskStartTime(TicketTaskDto ticketTaskObj);
    ArrayList<TicketTaskDto> getTaskAssigned(String assignedToID);
    boolean editTask(TicketTaskDto ticketTaskObj);
    boolean markTaskComplete(String ticketTaskID);
    boolean cancelTask(TicketTaskDto ticketTaskObj);
    ArrayList<TicketDto> getTicketsByCategory(String category);
    ArrayList<TicketDto> getTicketByStatus(String assignedToID, String status);
}
