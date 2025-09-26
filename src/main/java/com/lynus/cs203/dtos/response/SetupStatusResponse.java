package com.lynus.cs203.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetupStatusResponse {
    private boolean setupComplete;
    private String message;
}
