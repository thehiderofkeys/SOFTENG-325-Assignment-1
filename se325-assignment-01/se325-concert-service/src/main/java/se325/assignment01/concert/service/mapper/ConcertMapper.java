package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.ConcertDTO;
import se325.assignment01.concert.common.dto.ConcertSummaryDTO;
import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Concert;
import se325.assignment01.concert.service.domain.Performer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConcertMapper {
    public static Concert toDomainModel(ConcertDTO dto){
        return new Concert(
                dto.getId(),
                dto.getTitle(),
                dto.getImageName(),
                dto.getBlurb()
        );
    }
    public static ConcertDTO toConcertDTO(Concert concert){
        ConcertDTO dto = new ConcertDTO(
                concert.getId(),
                concert.getTitle(),
                concert.getImageName(),
                concert.getBlurb()
        );
        dto.setDates(new ArrayList<LocalDateTime>(concert.getDates()));
        dto.setPerformers(new ArrayList<>());
        for(Performer performer: concert.getPerformers()){
            dto.getPerformers().add(PerfomerMapper.toDTO(performer));
        }
        return dto;
    }
    public static ConcertSummaryDTO toConcertSummeryDTO(Concert concert){
        return new ConcertSummaryDTO(
                concert.getId(),
                concert.getTitle(),
                concert.getImageName()
        );
    }
}
