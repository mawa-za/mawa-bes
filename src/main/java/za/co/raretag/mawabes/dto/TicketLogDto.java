package za.co.raretag.mawabes.dto;

import java.util.ArrayList;

public class TicketLogDto {
    private String ticketLogID;
    private String ticketID;
    private String ticketTaskLogID;
    private String ticketTaskID;
    private String partnerID;
    private PersonDto partner;
    private String startTime;
    private String endTime;
    private String duration;
    private ArrayList<NoteDto> notes;
    private NoteDto note;

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

    public String getTicketTaskLogID() {
        return ticketTaskLogID;
    }

    public void setTicketTaskLogID(String ticketTaskLogID) {
        this.ticketTaskLogID = ticketTaskLogID;
    }

    public String getTicketTaskID() {
        return ticketTaskID;
    }

    public void setTicketTaskID(String ticketTaskID) {
        this.ticketTaskID = ticketTaskID;
    }

    public String getPartnerID() {
        return partnerID;
    }

    public void setPartnerID(String partnerID) {
        this.partnerID = partnerID;
    }

    public PersonDto getPartner() {
        return partner;
    }

    public void setPartner(PersonDto partner) {
        this.partner = partner;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public ArrayList<NoteDto> getNotes() {
        return notes;
    }

    public void setNotes(ArrayList<NoteDto> notes) {
        this.notes = notes;
    }

    public NoteDto getNote() {
        return note;
    }

    public void setNote(NoteDto note) {
        this.note = note;
    }
}
