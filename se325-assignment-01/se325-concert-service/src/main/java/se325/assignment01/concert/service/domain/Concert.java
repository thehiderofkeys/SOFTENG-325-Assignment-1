package se325.assignment01.concert.service.domain;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.*;

import javax.persistence.*;
@Entity
@Table(name = "CONCERTS")
public class Concert {
    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;
    @Column(name = "TITLE")
    private String title;
    @Column(name = "IMAGE_NAME")
    private String imageName;
    @Column(name = "BLURB",length = 4095)
    private String blurb;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "CONCERT_DATES", joinColumns = @JoinColumn(name = "CONCERT_ID"))
    @Column(name = "DATE")
    private Set<LocalDateTime> dates = new HashSet<>();
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "CONCERT_PERFORMER",
            joinColumns = @JoinColumn(name = "CONCERT_ID"),
            inverseJoinColumns = @JoinColumn(name = "PERFORMER_ID")
    )
    private Set<Performer> performers = new HashSet<>();

    public Concert() {
    }

    public Concert(Long id, String title, String imageName, String blurb) {
        this.id = id;
        this.title = title;
        this.imageName = imageName;
        this.blurb = blurb;
    }

    public Concert(String title, String imageName) {
        this.title = title;
        this.imageName = imageName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public Set<LocalDateTime> getDates() {
        return dates;
    }

    public void setDates(Set<LocalDateTime> dates) {
        this.dates = dates;
    }

    public Set<Performer> getPerformers() {
        return performers;
    }

    public void setPerformers(Set<Performer> performers) {
        this.performers = performers;
    }
}
