package com.itmao.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itmao.aispringboot.DTO.command.UserLoginCommandDTO;
import com.itmao.aispringboot.DTO.command.UserRegisterCommandDTO;
import com.itmao.aispringboot.DTO.command.UserApiKeyUpdateDTO;
import com.itmao.aispringboot.DTO.command.UserProfileUpdateDTO;
import com.itmao.aispringboot.DTO.response.UserLoginResponseDTO;
import com.itmao.aispringboot.DTO.response.UserProfileResponseDTO;
import com.itmao.aispringboot.common.ResultCode;
import com.itmao.aispringboot.config.JwtConfig;
import com.itmao.aispringboot.entity.AuthRefreshSession;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.mapper.AuthRefreshSessionMapper;
import com.itmao.aispringboot.mapper.UserMapper;
import com.itmao.aispringboot.service.convert.UserConvert;
import com.itmao.aispringboot.util.ApiKeyCrypto;
import com.itmao.aispringboot.util.JwtTokenUtil;
import com.itmao.aispringboot.util.ModelEndpointGuard;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private AuthRefreshSessionMapper authRefreshSessionMapper;
    @Resource
    private ConsultationMessageService consultationMessageService;
    @Resource
    private ApiKeyCrypto apiKeyCrypto;
    @Resource
    private ModelEndpointGuard modelEndpointGuard;
    @Resource
    private JwtConfig jwtConfig;

    @Value("${xiaoguang.free-chat-limit:20}")
    private int freeChatLimit;

    @Value("${spring.ai.openai.api-key:}")
    private String platformApiKey;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, commandDTO.getUsername())
                .or()
                .eq(User::getEmail, commandDTO.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException("账号或密码好像不对，再试一次？");
        }

        String inputPassword = commandDTO.getPassword().trim();
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BusinessException("账号或密码好像不对，再试一次？");
        }

        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        revokeExpiredRefreshSessions(user.getId());
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public UserLoginResponseDTO refresh(String refreshToken) {
        JwtTokenUtil.TokenVerificationResult token = JwtTokenUtil.validateRefreshToken(refreshToken);
        if (token == null || !token.isValid()) {
            throw invalidRefreshToken();
        }
        User user = userMapper.selectById(token.getUserId());
        if (user == null || !user.isActive() || currentTokenVersion(user) != token.getTokenVersion()) {
            throw invalidRefreshToken();
        }

        AuthRefreshSession session = authRefreshSessionMapper.selectById(token.getTokenId());
        if (session == null
                || !user.getId().equals(session.getUserId())
                || !token.getTokenVersion().equals(session.getTokenVersion())) {
            throw invalidRefreshToken();
        }
        if (session.getRevokedAt() != null) {
            // 已轮换令牌再次出现，按重放攻击处理，立即撤销该账号全部会话。
            invalidateAllSessions(user.getId());
            throw new BusinessException(ResultCode.TOKEN_BLOCKED.getCode(), "刷新令牌已被重复使用，请重新登录");
        }
        if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            revokeRefreshSession(session.getId());
            throw invalidRefreshToken();
        }

        int consumed = authRefreshSessionMapper.update(null,
                new LambdaUpdateWrapper<AuthRefreshSession>()
                        .eq(AuthRefreshSession::getId, session.getId())
                        .isNull(AuthRefreshSession::getRevokedAt)
                        .set(AuthRefreshSession::getRevokedAt, LocalDateTime.now()));
        if (consumed != 1) {
            invalidateAllSessions(user.getId());
            throw new BusinessException(ResultCode.TOKEN_BLOCKED.getCode(), "刷新令牌已被重复使用，请重新登录");
        }
        return issueTokens(user);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO) {
        if (!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入密码不一致");
        }

        LambdaQueryWrapper<User> userNameQuery = new LambdaQueryWrapper<>();
        userNameQuery.eq(User::getUsername, commandDTO.getUsername());
        if (userMapper.selectCount(userNameQuery) > 0) {
            throw new BusinessException("用户名已存在");
        }

        LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
        emailQuery.eq(User::getEmail, commandDTO.getEmail());
        if (userMapper.selectCount(emailQuery) > 0) {
            throw new BusinessException("邮箱已存在");
        }

        String encodedPassword = passwordEncoder.encode(commandDTO.getPassword().trim());
        User user = UserConvert.registerCommandToEntity(commandDTO, encodedPassword);
        userMapper.insert(user);
        return UserConvert.entityToDetailResponse(user);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserConvert.entityToDetailResponse(user);
    }

    public User getEntity(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("还没找到你的账号，先登录一下？");
        }
        return user;
    }

    public UserProfileResponseDTO getProfile(Long userId) {
        User user = getEntity(userId);
        // 仅在内存中解密，用于生成掩码展示，不会回写数据库
        if (user.hasOwnApiKey()) {
            user.setApiKey(apiKeyCrypto.decrypt(user.getApiKey()));
        }
        int used = consultationMessageService.countUserTurns(userId);
        return UserConvert.toProfileResponse(user, used, freeChatLimit, hasPlatformKey());
    }

    public UserProfileResponseDTO updateProfile(Long userId, UserProfileUpdateDTO dto) {
        User user = getEntity(userId);
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname().isBlank() ? null : dto.getNickname().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank() && !dto.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, dto.getEmail()).ne(User::getId, userId);
            if (userMapper.selectCount(emailQuery) > 0) {
                throw new BusinessException("这个邮箱已经被用过了");
            }
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getPhone() != null) {
            String phone = dto.getPhone().isBlank() ? null : dto.getPhone().trim();
            if (phone != null && !phone.equals(user.getPhone())) {
                LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
                phoneQuery.eq(User::getPhone, phone).ne(User::getId, userId);
                if (userMapper.selectCount(phoneQuery) > 0) {
                    throw new BusinessException("这个手机号已经被用过了");
                }
            }
            user.setPhone(phone);
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null) {
            user.setBirthday(dto.getBirthday());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar().isBlank() ? null : dto.getAvatar());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return getProfile(userId);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        User user = getEntity(userId);
        if (!passwordEncoder.matches(oldPassword == null ? "" : oldPassword.trim(), user.getPassword())) {
            throw new BusinessException("现在的密码不对");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setTokenVersion(currentTokenVersion(user) + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        revokeAllRefreshSessions(userId);
    }

    @Transactional
    public void logout(Long userId) {
        invalidateAllSessions(userId);
    }

    public UserProfileResponseDTO updateApiKey(Long userId, UserApiKeyUpdateDTO dto) {
        User user = getEntity(userId);
        if (Boolean.TRUE.equals(dto.getClearKey())) {
            user.setApiKey(null);
        } else if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            user.setApiKey(apiKeyCrypto.encrypt(dto.getApiKey().trim()));
        }
        if (dto.getApiBaseUrl() != null) {
            user.setApiBaseUrl(dto.getApiBaseUrl().isBlank() ? null : modelEndpointGuard.sanitizeBaseUrl(dto.getApiBaseUrl()));
        }
        if (dto.getApiModel() != null) {
            user.setApiModel(dto.getApiModel().isBlank() ? null : dto.getApiModel().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return getProfile(userId);
    }

    public void assertCanUsePlatformChat(User user) {
        if (user == null) {
            throw new BusinessException("还没找到你的账号，先登录一下？");
        }
        if (user.hasOwnApiKey()) {
            return;
        }
        if (!hasPlatformKey()) {
            throw new BusinessException("还没有可用的模型密钥。去「我的」里填上你自己的 API Key，小光就能陪你。");
        }
        if (user.getUserType() != null && user.getUserType() == 2) {
            return;
        }
        int used = consultationMessageService.countUserTurns(user.getId());
        if (used >= freeChatLimit) {
            throw new BusinessException(quotaExceededMessage());
        }
    }

    public String quotaExceededMessage() {
        if (!hasPlatformKey()) {
            return "还没有可用的模型密钥。去「我的」里填上你自己的 API Key，小光就能陪你。";
        }
        return "免费的 " + freeChatLimit + " 次已经用完了。去「我的」里填上你自己的 API Key，小光就能继续陪你。";
    }

    public boolean canUsePlatformChat(User user, boolean upcomingCounts) {
        if (user == null) {
            return false;
        }
        if (user.hasOwnApiKey()) {
            return true;
        }
        if (!hasPlatformKey()) {
            return false;
        }
        if (user.getUserType() != null && user.getUserType() == 2) {
            return true;
        }
        int used = consultationMessageService.countUserTurns(user.getId());
        int wouldBe = upcomingCounts ? used + 1 : used;
        return wouldBe <= freeChatLimit;
    }

    private boolean hasPlatformKey() {
        return platformApiKey != null
                && !platformApiKey.isBlank()
                && !"not-configured".equals(platformApiKey);
    }

    private UserLoginResponseDTO issueTokens(User user) {
        int tokenVersion = currentTokenVersion(user);
        String refreshId = UUID.randomUUID().toString();
        String accessToken = JwtTokenUtil.generateAccessToken(
                user.getId(), user.getUsername(), user.getUserType(), tokenVersion);
        String refreshToken = JwtTokenUtil.generateRefreshToken(
                user.getId(), user.getUsername(), user.getUserType(), tokenVersion, refreshId);

        LocalDateTime now = LocalDateTime.now();
        authRefreshSessionMapper.insert(AuthRefreshSession.builder()
                .id(refreshId)
                .userId(user.getId())
                .tokenVersion(tokenVersion)
                .expiresAt(now.plus(Duration.ofMillis(jwtConfig.getRefreshExpiration())))
                .createdAt(now)
                .build());

        return UserConvert.entityToLoginResponse(
                accessToken,
                refreshToken,
                jwtConfig.getExpiration(),
                UserConvert.entityToDetailResponse(user));
    }

    private int currentTokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private void invalidateAllSessions(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .setSql("token_version = COALESCE(token_version, 0) + 1")
                .set(User::getUpdatedAt, now));
        revokeAllRefreshSessions(userId);
    }

    private void revokeAllRefreshSessions(Long userId) {
        authRefreshSessionMapper.update(null, new LambdaUpdateWrapper<AuthRefreshSession>()
                .eq(AuthRefreshSession::getUserId, userId)
                .isNull(AuthRefreshSession::getRevokedAt)
                .set(AuthRefreshSession::getRevokedAt, LocalDateTime.now()));
    }

    private void revokeRefreshSession(String tokenId) {
        authRefreshSessionMapper.update(null, new LambdaUpdateWrapper<AuthRefreshSession>()
                .eq(AuthRefreshSession::getId, tokenId)
                .isNull(AuthRefreshSession::getRevokedAt)
                .set(AuthRefreshSession::getRevokedAt, LocalDateTime.now()));
    }

    private void revokeExpiredRefreshSessions(Long userId) {
        authRefreshSessionMapper.delete(new LambdaQueryWrapper<AuthRefreshSession>()
                .eq(AuthRefreshSession::getUserId, userId)
                .lt(AuthRefreshSession::getExpiresAt, LocalDateTime.now()));
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(ResultCode.TOKEN_INVALID.getCode(), "刷新令牌无效或已经过期，请重新登录");
    }
}
