package se325.assignment01.concert.service.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

/***
 * The Key Object for utilising a Compound Key.
 */
public class SeatKey implements Serializable {
    private String label;
    private LocalDateTime date;
}
