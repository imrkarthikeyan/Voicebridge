package com.voicebridge.mapper;

import com.voicebridge.dto.response.SpeakingRequestResponse;
import com.voicebridge.entity.SpeakingRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SpeakingRequestMapper {

    @Mapping(target = "participantId", source = "participant.id")
    @Mapping(target = "participantName", source = "participant.name")
    @Mapping(target = "queueOrder", source = "queuePosition")
    SpeakingRequestResponse toResponse(SpeakingRequest speakingRequest);
}
