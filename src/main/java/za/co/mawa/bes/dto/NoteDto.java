package za.co.mawa.bes.dto;

import java.io.Serializable;
import java.util.ArrayList;

public class NoteDto implements Serializable {


    private String id;
    private String value;
    private String date_created;
    private String transaction;
    private String type;
    private String dateCreated;
    private String timeCreated;
    private String loggedById;
    private PersonDto loggedBy;
    private ArrayList<String> recievedBy;
    private ArrayList<String> recievers;
    private boolean sms;
    private boolean email;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDate_created() {
        return date_created;
    }

    public void setDate_created(String date_created) {
        this.date_created = date_created;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getLoggedById() {
        return loggedById;
    }

    public void setLoggedById(String loggedById) {
        this.loggedById = loggedById;
    }

    public PersonDto getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(PersonDto loggedBy) {
        this.loggedBy = loggedBy;
    }

    public ArrayList<String> getRecievedBy() {
        return recievedBy;
    }

    public void setRecievedBy(ArrayList<String> recievedBy) {
        this.recievedBy = recievedBy;
    }

    public ArrayList<String> getRecievers() {
        return recievers;
    }

    public void setRecievers(ArrayList<String> recievers) {
        this.recievers = recievers;
    }

    public boolean isSms() {
        return sms;
    }

    public void setSms(boolean sms) {
        this.sms = sms;
    }

    public boolean isEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }
}
