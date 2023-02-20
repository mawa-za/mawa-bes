package za.co.mawa.bes.entity;

import jakarta.persistence.*;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FieldOptionPKEntity implements Serializable {
    @Column(name = "field")
    private String field;

    @Column(name = "code")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldOptionPKEntity that = (FieldOptionPKEntity) o;
        return Objects.equals(field, that.field) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, code);
    }
}
