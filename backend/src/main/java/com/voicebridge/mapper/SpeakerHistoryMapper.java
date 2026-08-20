package com.voicebridge.mapper;

import com.voicebridge.dto.response.SpeakerHistoryResponse;
import com.voicebridge.entity.SpeakerHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SpeakerHistoryMapper {
    @Mapping(target = "meetingId", source = "meeting.id")
    @Mapping(target = "participantId", source = "participant.id")
    SpeakerHistoryResponse toResponse(SpeakerHistory speakerHistory);
}
