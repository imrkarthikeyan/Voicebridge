package com.voicebridge.mapper;

import com.voicebridge.dto.response.PresentationSessionResponse;
import com.voicebridge.entity.PresentationSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PresentationSessionMapper {
    @Mapping(target = "meetingId", source = "meeting.id")
    @Mapping(target = "presentationId", source = "presentation.id")
    PresentationSessionResponse toResponse(PresentationSession session);
}
