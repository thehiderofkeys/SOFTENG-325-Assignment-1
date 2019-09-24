package se325.assignment01.concert.service.domain;
import javax.persistence.*;

@Entity
@Table(name = "USERS")
public class User {
    @Id
    @GeneratedValue
    @Column(name = "ID")
    private long id;
    @Column(name = "USERNAME")
    private String username;
    @Column(name = "PASSWORD")
    private String password;
    @Version
    @Column(name = "VERSION")
    private long version;

    private Integer hash;

    protected User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.hash = this.hashCode();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }
    @Override
    public int hashCode() {
        return (username.hashCode()+password.hashCode());
    }
}
