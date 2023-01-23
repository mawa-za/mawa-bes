package za.co.mawa.bes.dto;

import java.util.ArrayList;

public class OrderPartnerDto {
    private String partner;
    private String transaction;
    private String function;
    private String dateAdded;
    private String dateJoined;
    private String dateEffective;
    private String validFrom;
    private String status;
    private String statusReason;
    private String createdBy;
    private String changedBy;
    private ArrayList<String> approvers;
    private String approverToEdit;
    private String approver;

//    public OrderPartnerDto(String partner, String transaction, String function,
//                           String dateAdded, String dateJoined, String dateEffective,
//                           String validFrom, String status, String statusReason, String createdBy,
//                           String changedBy, ArrayList<String> approvers, String approverToEdit, String approver) {
//        this.partner = partner;
//        this.transaction = transaction;
//        this.function = function;
//        this.dateAdded = dateAdded;
//        this.dateJoined = dateJoined;
//        this.dateEffective = dateEffective;
//        this.validFrom = validFrom;
//        this.status = status;
//        this.statusReason = statusReason;
//        this.createdBy = createdBy;
//        this.changedBy = changedBy;
//        this.approvers = approvers;
//        this.approverToEdit = approverToEdit;
//        this.approver = approver;
//    }

    public String getPartner() {
        return partner;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getDateJoined() {
        return dateJoined;
    }

    public void setDateJoined(String dateJoined) {
        this.dateJoined = dateJoined;
    }

    public String getDateEffective() {
        return dateEffective;
    }

    public void setDateEffective(String dateEffective) {
        this.dateEffective = dateEffective;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public ArrayList<String> getApprovers() {
        return approvers;
    }

    public void setApprovers(ArrayList<String> approvers) {
        this.approvers = approvers;
    }

    public String getApproverToEdit() {
        return approverToEdit;
    }

    public void setApproverToEdit(String approverToEdit) {
        this.approverToEdit = approverToEdit;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }
}
