package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Performer;


/***
 * Helper class to map Performer DTO to Domain Model.
 */
public class PerformerMapper {
    public static Performer toDomainModel(PerformerDTO dto){
        Performer performer = new Performer(dto.getId(),dto.getName(),dto.getImageName(),dto.getGenre(),dto.getBlurb());
        return performer;
    }
    public static PerformerDTO toDTO(Performer performer){
        PerformerDTO dto = new PerformerDTO(
                performer.getId(),
                performer.getName(),
                performer.getImageName(),
                performer.getGenre(),
                performer.getBlurb()
        );
        return dto;
    }
}
