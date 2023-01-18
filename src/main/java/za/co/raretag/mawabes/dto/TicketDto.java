package za.co.raretag.mawabes.dto;

import java.util.ArrayList;

public class TicketDto {
    private String id;
    private String status;
    private String priority;
    private String dateLogged;
    private String dueDate;
    private String cancelDate;
    private String resolveDate;
    private String completedDate;
    private String category;
    private String description;
    private String subDescription;
    private String assignedByID;
    private PersonDto assignedBy;
    private String assignGroupID;
    private String assignedToID;
    private ArrayList<String> approvers;
    private ArrayList<PersonDto> assignedTo;
    private String clientId;
    private String duration;
    private String timeSpent;
    private String ticketTaskID;

//    public TicketDto(String id, String status, String priority, String dateLogged,
//                     String dueDate, String cancelDate, String resolveDate, String completedDate,
//                     String category, String description, String subDescription, String assignedByID,
//                     PersonDto assignedBy, String assignGroupID, String assignedToID, ArrayList<String> approvers,
//                     ArrayList<PersonDto> assignedTo, String clientId, String duration, String timeSpent, String ticketTaskID) {
//        this.id = id;
//        this.status = status;
//        this.priority = priority;
//        this.dateLogged = dateLogged;
//        this.dueDate = dueDate;
//        this.cancelDate = cancelDate;
//        this.resolveDate = resolveDate;
//        this.completedDate = completedDate;
//        this.category = category;
//        this.description = description;
//        this.subDescription = subDescription;
//        this.assignedByID = assignedByID;
//        this.assignedBy = assignedBy;
//        this.assignGroupID = assignGroupID;
//        this.assignedToID = assignedToID;
//        this.approvers = approvers;
//        this.assignedTo = assignedTo;
//        this.clientId = clientId;
//        this.duration = duration;
//        this.timeSpent = timeSpent;
//        this.ticketTaskID = ticketTaskID;
//    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDateLogged() {
        return dateLogged;
    }

    public void setDateLogged(String dateLogged) {
        this.dateLogged = dateLogged;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getCancelDate() {
        return cancelDate;
    }

    public void setCancelDate(String cancelDate) {
        this.cancelDate = cancelDate;
    }

    public String getResolveDate() {
        return resolveDate;
    }

    public void setResolveDate(String resolveDate) {
        this.resolveDate = resolveDate;
    }

    public String getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(String completedDate) {
        this.completedDate = completedDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubDescription() {
        return subDescription;
    }

    public void setSubDescription(String subDescription) {
        this.subDescription = subDescription;
    }

    public String getAssignedByID() {
        return assignedByID;
    }

    public void setAssignedByID(String assignedByID) {
        this.assignedByID = assignedByID;
    }

    public PersonDto getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(PersonDto assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getAssignGroupID() {
        return assignGroupID;
    }

    public void setAssignGroupID(String assignGroupID) {
        this.assignGroupID = assignGroupID;
    }

    public String getAssignedToID() {
        return assignedToID;
    }

    public void setAssignedToID(String assignedToID) {
        this.assignedToID = assignedToID;
    }

    public ArrayList<String> getApprovers() {
        return approvers;
    }

    public void setApprovers(ArrayList<String> approvers) {
        this.approvers = approvers;
    }

    public ArrayList<PersonDto> getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(ArrayList<PersonDto> assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(String timeSpent) {
        this.timeSpent = timeSpent;
    }

    public String getTicketTaskID() {
        return ticketTaskID;
    }

    public void setTicketTaskID(String ticketTaskID) {
        this.ticketTaskID = ticketTaskID;
    }
}
