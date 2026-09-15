package com.itmao.aispringboot.controller;

import com.itmao.aispringboot.DTO.command.UserApiKeyUpdateDTO;
import com.itmao.aispringboot.DTO.command.UserLoginCommandDTO;
import com.itmao.aispringboot.DTO.command.UserPasswordChangeDTO;
import com.itmao.aispringboot.DTO.command.UserProfileUpdateDTO;
import com.itmao.aispringboot.DTO.command.UserRegisterCommandDTO;
import com.itmao.aispringboot.DTO.response.UserLoginResponseDTO;
import com.itmao.aispringboot.DTO.response.UserProfileResponseDTO;
import com.itmao.aispringboot.common.Result;
import com.itmao.aispringboot.service.UserService;
import com.itmao.aispringboot.util.JwtTokenUtil;
import com.itmao.aispringboot.util.RefreshTokenCookie;
import com.itmao.aispringboot.util.RequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/user")
public class User {

    private final UserService userService;
    private final RequestRateLimiter requestRateLimiter;
    private final RefreshTokenCookie refreshTokenCookie;

    public User(UserService userService, RequestRateLimiter requestRateLimiter,
                RefreshTokenCookie refreshTokenCookie) {
        this.userService = userService;
        this.requestRateLimiter = requestRateLimiter;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        String ip = requestRateLimiter.clientIp(request);
        requestRateLimiter.assertAllowed("login-ip:" + ip, 12, Duration.ofMinutes(15));
        requestRateLimiter.assertAllowed("login-user:" + ip + ":" + commandDTO.getUsername(), 8, Duration.ofMinutes(15));
        UserLoginResponseDTO result = userService.login(commandDTO);
        refreshTokenCookie.write(response, result.getRefreshToken());
        return Result.success(result);
    }

    @PostMapping("/refresh")
    public Result<UserLoginResponseDTO> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = refreshTokenCookie.read(request);
        try {
            UserLoginResponseDTO result = userService.refresh(refreshToken);
            refreshTokenCookie.write(response, result.getRefreshToken());
            return Result.success(result);
        } catch (RuntimeException e) {
            refreshTokenCookie.clear(response);
            throw e;
        }
    }

    @PostMapping("/add")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO commandDTO,
                                                                       HttpServletRequest request) {
        requestRateLimiter.assertAllowed("register-ip:" + requestRateLimiter.clientIp(request), 5, Duration.ofHours(1));
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.register(commandDTO);
        return Result.success(result);
    }

    @GetMapping("/current")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser() {
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.getUserById(JwtTokenUtil.getCurrentUserId());
        return Result.success(result);
    }

    @GetMapping("/profile")
    public Result<UserProfileResponseDTO> getProfile() {
        return Result.success(userService.getProfile(JwtTokenUtil.getCurrentUserId()));
    }

    @PutMapping("/profile")
    public Result<UserProfileResponseDTO> updateProfile(@Valid @RequestBody UserProfileUpdateDTO dto) {
        return Result.success(userService.updateProfile(JwtTokenUtil.getCurrentUserId(), dto));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody UserPasswordChangeDTO dto) {
        userService.changePassword(
                JwtTokenUtil.getCurrentUserId(),
                dto.getOldPassword(),
                dto.getNewPassword(),
                dto.getConfirmPassword()
        );
        return Result.success();
    }

    @PutMapping("/api-key")
    public Result<UserProfileResponseDTO> updateApiKey(@Valid @RequestBody UserApiKeyUpdateDTO dto) {
        return Result.success(userService.updateApiKey(JwtTokenUtil.getCurrentUserId(), dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletResponse response) {
        userService.logout(JwtTokenUtil.getCurrentUserId());
        refreshTokenCookie.clear(response);
        return Result.success();
    }
}
