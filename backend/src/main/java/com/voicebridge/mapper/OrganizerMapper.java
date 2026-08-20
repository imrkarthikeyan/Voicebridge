package com.voicebridge.mapper;

import com.voicebridge.dto.response.OrganizerSummaryResponse;
import com.voicebridge.entity.Organizer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizerMapper {

    OrganizerSummaryResponse toSummaryResponse(Organizer organizer);
}
