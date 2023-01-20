package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
@Embeddable
public class FieldOptionPKEntity implements Serializable {
    @Column(name = "field", length = 20)
    private String field;

    @Column(name = "code", length = 20)
    private String code;

    public FieldOptionPKEntity() {
    }

    public FieldOptionPKEntity(String field, String code) {
        this.field = field;
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
