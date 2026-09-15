package com.itmao.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer gender;
    private String genderDisplayName;
    private LocalDate birthday;
    private Integer userType;
    private String displayName;
    private boolean hasApiKey;
    private String apiKeyMasked;
    private String apiBaseUrl;
    private String apiModel;
    private int freeLimit;
    private int usedCount;
    private int remainingCount;
    private boolean unlimited;
    private boolean platformKeyConfigured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
