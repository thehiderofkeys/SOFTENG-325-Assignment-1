package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.service.domain.Seat;


/***
 * Helper class to map Seat DTO to Domain Model.
 */
public class SeatMapper {
    public static SeatDTO toDTO(Seat seat){
        SeatDTO dto = new SeatDTO(seat.getLabel(), seat.getPrice());
        return dto;
    }
}