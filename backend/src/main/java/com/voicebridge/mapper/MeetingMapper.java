package com.voicebridge.mapper;

import com.voicebridge.dto.response.MeetingResponse;
import com.voicebridge.entity.Meeting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MeetingMapper {

    @Mapping(target = "joinUrl", ignore = true)
    @Mapping(target = "qrCodeUrl", ignore = true)
    @Mapping(target = "closedAt", source = "endedAt")
    MeetingResponse toResponse(Meeting meeting);
}
