package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.common.dto.SeatDTO;
import se325.assignment01.concert.service.domain.Booking;
import se325.assignment01.concert.service.domain.Seat;

import java.util.LinkedList;
import java.util.List;

public class BookingMapper {
    public static BookingDTO toDTO(Booking booking){
        BookingDTO dto = new BookingDTO();
        dto.setConcertId(booking.getConcertId());
        dto.setDate(booking.getDate());
        List<SeatDTO> seatDTOs = new LinkedList<>();
        dto.setSeats(seatDTOs);
        for(Seat seat: booking.getSeats())
            seatDTOs.add(SeatMapper.toDTO(seat));
        return dto;
    }

    public static Booking toDomainModel(BookingDTO dto){
        Booking booking = new Booking();
        booking.setConcertId(dto.getConcertId());
        booking.setDate(dto.getDate());
        List<Seat> seats = new LinkedList<>();
        booking.setSeats(seats);
        for(SeatDTO seatDTO: dto.getSeats()){}
            //seats.add(SeatMapper.toDomainModel(seatDTO));
        return booking;
    }
}
