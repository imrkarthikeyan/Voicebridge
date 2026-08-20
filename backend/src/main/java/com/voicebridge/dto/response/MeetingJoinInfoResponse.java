package com.voicebridge.dto.response;

import com.voicebridge.entity.enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingJoinInfoResponse {

    private String meetingCode;
    private String title;
    private MeetingStatus status;
}
