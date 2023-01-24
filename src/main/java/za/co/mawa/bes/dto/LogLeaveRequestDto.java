package za.co.mawa.bes.dto;

import java.util.ArrayList;

public class LogLeaveRequestDto {

    private String id;
    private String startDate;
    private String endDate;
    private String leaveType;
    private String subType;
    private String duration;
    private String status;
    private String dateCreated;
    private String description;
    private String subDescription;
    private String unitOfMeasure;
    private String productCode;
    private String quantity;
    private String quantityRemaining;
    private String statusReason;
    private String loggedByID;
    private String approverID;
    private String leaveTypeDescription;
    private NoteDto note;
    private PersonDto approvedBy;
    private PersonDto loggedBy;
    private ArrayList<NoteDto> notes;

    public LogLeaveRequestDto() {
    }

    public LogLeaveRequestDto(String id, String startDate, String endDate, String leaveType, String subType,
                              String duration, String status, String dateCreated, String description,
                              String subDescription, String unitOfMeasure, String productCode, String quantity,
                              String quantityRemaining, String statusReason, String loggedByID, String approverID,
                              String leaveTypeDescription, NoteDto note, PersonDto approvedBy, PersonDto loggedBy, ArrayList<NoteDto> notes) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.leaveType = leaveType;
        this.subType = subType;
        this.duration = duration;
        this.status = status;
        this.dateCreated = dateCreated;
        this.description = description;
        this.subDescription = subDescription;
        this.unitOfMeasure = unitOfMeasure;
        this.productCode = productCode;
        this.quantity = quantity;
        this.quantityRemaining = quantityRemaining;
        this.statusReason = statusReason;
        this.loggedByID = loggedByID;
        this.approverID = approverID;
        this.leaveTypeDescription = leaveTypeDescription;
        this.note = note;
        this.approvedBy = approvedBy;
        this.loggedBy = loggedBy;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
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

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getQuantityRemaining() {
        return quantityRemaining;
    }

    public void setQuantityRemaining(String quantityRemaining) {
        this.quantityRemaining = quantityRemaining;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getLoggedByID() {
        return loggedByID;
    }

    public void setLoggedByID(String loggedByID) {
        this.loggedByID = loggedByID;
    }

    public String getApproverID() {
        return approverID;
    }

    public void setApproverID(String approverID) {
        this.approverID = approverID;
    }

    public String getLeaveTypeDescription() {
        return leaveTypeDescription;
    }

    public void setLeaveTypeDescription(String leaveTypeDescription) {
        this.leaveTypeDescription = leaveTypeDescription;
    }

    public NoteDto getNote() {
        return note;
    }

    public void setNote(NoteDto note) {
        this.note = note;
    }

    public PersonDto getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(PersonDto approvedBy) {
        this.approvedBy = approvedBy;
    }

    public PersonDto getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(PersonDto loggedBy) {
        this.loggedBy = loggedBy;
    }

    public ArrayList<NoteDto> getNotes() {
        return notes;
    }

    public void setNotes(ArrayList<NoteDto> notes) {
        this.notes = notes;
    }
}
