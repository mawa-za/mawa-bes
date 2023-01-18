package za.co.raretag.mawabes.dto;

import java.util.ArrayList;

public class TicketTaskDto {
    private String ticketTaskID;
    private String ticketID;
    private String assignedTo;
    private PersonDto personAssignTo;
    private String assignedBy;
    private PersonDto personAssignedBy;
    private String taskLogLoggedBy;
    private String taskLogID;
    private ArrayList<TicketLogDto> taskLogs;
    private String startTime;
    private String endTime;
    private String dueDate;
    private String subType;
    private String type;
    private String summary;
    private String taskDescription;
    private String status;
    private String duration;
    private ArrayList<NoteDto> notes;
    private NoteDto note;

    public String getTicketTaskID() {
        return ticketTaskID;
    }

    public void setTicketTaskID(String ticketTaskID) {
        this.ticketTaskID = ticketTaskID;
    }

    public String getTicketID() {
        return ticketID;
    }

    public void setTicketID(String ticketID) {
        this.ticketID = ticketID;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public PersonDto getPersonAssignTo() {
        return personAssignTo;
    }

    public void setPersonAssignTo(PersonDto personAssignTo) {
        this.personAssignTo = personAssignTo;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public PersonDto getPersonAssignedBy() {
        return personAssignedBy;
    }

    public void setPersonAssignedBy(PersonDto personAssignedBy) {
        this.personAssignedBy = personAssignedBy;
    }

    public String getTaskLogLoggedBy() {
        return taskLogLoggedBy;
    }

    public void setTaskLogLoggedBy(String taskLogLoggedBy) {
        this.taskLogLoggedBy = taskLogLoggedBy;
    }

    public String getTaskLogID() {
        return taskLogID;
    }

    public void setTaskLogID(String taskLogID) {
        this.taskLogID = taskLogID;
    }

    public ArrayList<TicketLogDto> getTaskLogs() {
        return taskLogs;
    }

    public void setTaskLogs(ArrayList<TicketLogDto> taskLogs) {
        this.taskLogs = taskLogs;
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

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
