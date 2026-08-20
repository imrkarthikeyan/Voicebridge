package com.voicebridge.mapper;

import com.voicebridge.dto.response.PresentationResponse;
import com.voicebridge.entity.Presentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PresentationMapper {
    @Mapping(target = "meetingId", source = "meeting.id")
    PresentationResponse toResponse(Presentation presentation);
}
