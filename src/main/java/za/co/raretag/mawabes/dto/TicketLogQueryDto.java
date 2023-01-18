package za.co.raretag.mawabes.dto;

public class TicketLogQueryDto {
    private String ticketLogID;
    private String ticketID;
    private String ticketTaskID;

    public String getTicketLogID() {
        return ticketLogID;
    }

    public void setTicketLogID(String ticketLogID) {
        this.ticketLogID = ticketLogID;
    }

    public String getTicketID() {
        return ticketID;
    }

    public void setTicketID(String ticketID) {
        this.ticketID = ticketID;
    }

    public String getTicketTaskID() {
        return ticketTaskID;
    }

    public void setTicketTaskID(String ticketTaskID) {
        this.ticketTaskID = ticketTaskID;
    }
}
