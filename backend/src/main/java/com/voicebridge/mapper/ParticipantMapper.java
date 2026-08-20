package com.voicebridge.mapper;

import com.voicebridge.dto.response.ParticipantResponse;
import com.voicebridge.entity.Participant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParticipantMapper {

    @Mapping(target = "meetingCode", source = "meeting.meetingCode")
    ParticipantResponse toResponse(Participant participant);
}
