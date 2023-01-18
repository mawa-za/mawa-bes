package za.co.raretag.mawabes.dao;

import za.co.raretag.mawabes.dto.TicketLogDto;
import za.co.raretag.mawabes.dto.TicketLogQueryDto;

import java.util.ArrayList;

public interface TicketLogDao {
    String create(TicketLogDto ticketObj);
    boolean endDate(String ticketLogID);
    ArrayList<TicketLogDto> getLogByTicket(String ticketTaskID);
    boolean removeLogs(String ticketID);
    TicketLogDto getLog(TicketLogQueryDto ticketLogQueryObj);
    boolean removeTicketLog(TicketLogQueryDto ticketObj);
    ArrayList< TicketLogDto> getAllLogs();
}
