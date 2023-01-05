package za.co.raretag.mawabes.entity;

import javax.persistence.*;

@Entity
@Table(name = "user")
public class UserEntity {
    @Id
    @Column
    private String id;
    @Column
    private String password;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
