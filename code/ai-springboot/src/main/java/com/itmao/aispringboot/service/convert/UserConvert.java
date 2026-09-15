package com.itmao.aispringboot.service.convert;

import com.itmao.aispringboot.DTO.command.UserRegisterCommandDTO;
import com.itmao.aispringboot.DTO.response.UserLoginResponseDTO;
import com.itmao.aispringboot.DTO.response.UserProfileResponseDTO;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.enumClass.UserStatus;
import com.itmao.aispringboot.enumClass.UserType;

import java.time.LocalDateTime;

public class UserConvert {
    //相应DTO
    public static UserLoginResponseDTO.UserDetailResponseDTO entityToDetailResponse(User user) {
        return UserLoginResponseDTO.UserDetailResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .gender(user.getGender())
                .genderDisplayName(getGenderDisplayName(user.getGender()))
                .birthday(user.getBirthday())
                .userType(user.getUserType())
                .userTypeDisplayName(user.getUserTypeDisplayName())
                .status(user.getStatus())
                .statusDisplayName(user.getStatusDisplayName())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static UserProfileResponseDTO toProfileResponse(User user, int usedCount, int freeLimit, boolean platformKeyConfigured) {
        boolean unlimited = (user.getUserType() != null && user.getUserType() == 2) || user.hasOwnApiKey();
        int remaining = unlimited ? freeLimit : Math.max(0, freeLimit - usedCount);
        return UserProfileResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .gender(user.getGender())
                .genderDisplayName(getGenderDisplayName(user.getGender()))
                .birthday(user.getBirthday())
                .userType(user.getUserType())
                .displayName(user.getDisplayName())
                .hasApiKey(user.hasOwnApiKey())
                .apiKeyMasked(maskApiKey(user.getApiKey()))
                .apiBaseUrl(user.getApiBaseUrl())
                .apiModel(user.getApiModel())
                .freeLimit(freeLimit)
                .usedCount(usedCount)
                .remainingCount(remaining)
                .unlimited(unlimited)
                .platformKeyConfigured(platformKeyConfigured)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        String key = apiKey.trim();
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    public static UserLoginResponseDTO entityToLoginResponse(
            String token,
            String refreshToken,
            long expiresIn,
            UserLoginResponseDTO.UserDetailResponseDTO userInfo) {
        return UserLoginResponseDTO.builder()
                .userInfo(userInfo)
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .roleType(userInfo.getUserType().toString())
                .build();
    }

    public static User registerCommandToEntity(UserRegisterCommandDTO commandDTO, String encodedPassword) {
        String phone = commandDTO.getPhone();
        if (phone != null && phone.isBlank()) {
            phone = null;
        }
        return User.builder()
                .username(commandDTO.getUsername())
                .email(commandDTO.getEmail())
                .password(encodedPassword)
                .nickname(commandDTO.getNickname())
                .phone(phone)
                .gender(commandDTO.getGender() != null ? commandDTO.getGender() : 0)
                .birthday(commandDTO.getBirthday())
                .userType(UserType.USER.getCode())
                .status(UserStatus.NORMAL.getCode())
                .tokenVersion(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 获取性别显示名称
     * @param gender 性别代码
     * @return 性别显示名称
     */
    private static String getGenderDisplayName(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 1:
                return "男";
            case 2:
                return "女";
            default:
                return "未知";
        }


    }
}
