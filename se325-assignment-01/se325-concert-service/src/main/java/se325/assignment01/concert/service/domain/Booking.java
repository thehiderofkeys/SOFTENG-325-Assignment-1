package se325.assignment01.concert.service.domain;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/***
 * Booking Domain Model.
 */
@Entity
public class Booking {

    @Id
    @GeneratedValue
    private long id;
    private long concertId;
    private LocalDateTime date;
    @OneToMany(fetch = FetchType.EAGER)
    private List<Seat> seats = new ArrayList<>();
    @ManyToOne
    private User user;

    public Booking() {

    }

    public Booking(long id, long concertId, LocalDateTime date, List<Seat> seats, User user) {
        this.concertId = concertId;
        this.date = date;
        this.seats = seats;
        this.user = user;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getConcertId() {
        return concertId;
    }

    public void setConcertId(long concertId) {
        this.concertId = concertId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
