package za.co.mawa.bes.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "employment")
//@XmlRootElement
//@NamedQueries({
//        @NamedQuery(name = "Employment.findAll", query = "SELECT e FROM Employment e")
//        , @NamedQuery(name = "Employment.findByEmployeeId", query = "SELECT e FROM Employment e WHERE e.employmentPK.employeeId = :employeeId")
//        , @NamedQuery(name = "Employment.findByStartDate", query = "SELECT e FROM Employment e WHERE e.employmentPK.startDate = :startDate")
//        , @NamedQuery(name = "Employment.findByEndDate", query = "SELECT e FROM Employment e WHERE e.endDate = :endDate")
//        , @NamedQuery(name = "Employment.findByPosition", query = "SELECT e FROM Employment e WHERE e.position = :position")
//        , @NamedQuery(name = "Employment.findByStatus", query = "SELECT e FROM Employment e WHERE e.status = :status")
//        , @NamedQuery(name = "Employment.findByType", query = "SELECT e FROM Employment e WHERE e.type = :type")
//        , @NamedQuery(name = "Employment.findByBranch", query = "SELECT e FROM Employment e WHERE e.branch = :branch")
//        , @NamedQuery(name = "Employment.findByDepartment", query = "SELECT e FROM Employment e WHERE e.department = :department")})
public class EmploymentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected EmploymentPKEntity employmentPK;
    @Column(name = "end_date")
    @Temporal(TemporalType.DATE)
    private Date endDate;
    //@Size(max = 10)
    @Column(name = "position", length = 20)
    private String position;
    //@Size(max = 5)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "branch", length = 20)
    private String branch;

    @Column(name = "department", length = 20)
    private String department;

    public EmploymentEntity() {
    }

    public EmploymentEntity(EmploymentPKEntity employmentPK) {
        this.employmentPK = employmentPK;
    }

    public EmploymentEntity(String employeeId, Date startDate) {
        this.employmentPK = new EmploymentPKEntity(employeeId, startDate);
    }

    public EmploymentPKEntity getEmploymentPK() {
        return employmentPK;
    }

    public void setEmploymentPK(EmploymentPKEntity employmentPK) {
        this.employmentPK = employmentPK;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getBranch() {
        return branch;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (employmentPK != null ? employmentPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EmploymentEntity)) {
            return false;
        }
        EmploymentEntity other = (EmploymentEntity) object;
        if ((this.employmentPK == null && other.employmentPK != null) || (this.employmentPK != null && !this.employmentPK.equals(other.employmentPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "za.co.raretag.mawa.entities.Employment[ employmentPK=" + employmentPK + " ]";
    }
}
