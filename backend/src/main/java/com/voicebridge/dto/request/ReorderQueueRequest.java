package com.voicebridge.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderQueueRequest {

    @NotEmpty(message = "orderedRequestIds must not be empty")
    private List<Long> orderedRequestIds;
}
