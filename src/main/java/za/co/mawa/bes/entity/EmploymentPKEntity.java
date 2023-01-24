package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Embeddable
public class EmploymentPKEntity implements Serializable {
    @Basic(optional = false)
    //@NotNull
    //@Size(min = 1, max = 10)
    @Column(name = "employee_id", length = 20)
    private String employeeId;
    @Basic(optional = false)
    //@NotNull
    @Column(name = "start_date")
    @Temporal(TemporalType.DATE)
    private Date startDate;

    public EmploymentPKEntity() {
    }

    public EmploymentPKEntity(String employeeId, Date startDate) {
        this.employeeId = employeeId;
        this.startDate = startDate;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (employeeId != null ? employeeId.hashCode() : 0);
        hash += (startDate != null ? startDate.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EmploymentPKEntity)) {
            return false;
        }
        EmploymentPKEntity other = (EmploymentPKEntity) object;
        if ((this.employeeId == null && other.employeeId != null) || (this.employeeId != null && !this.employeeId.equals(other.employeeId))) {
            return false;
        }
        if ((this.startDate == null && other.startDate != null) || (this.startDate != null && !this.startDate.equals(other.startDate))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.co.raretag.mawa.entities.EmploymentPK[ employeeId=" + employeeId + ", startDate=" + startDate + " ]";
    }
}
