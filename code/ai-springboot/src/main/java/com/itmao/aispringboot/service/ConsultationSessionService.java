package com.itmao.aispringboot.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itmao.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.itmao.aispringboot.DTO.query.PageQueryDTO;
import com.itmao.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.itmao.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import com.itmao.aispringboot.entity.ConsultationMessage;
import com.itmao.aispringboot.entity.ConsultationSession;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.mapper.ConsultationSessionMapper;
import com.itmao.aispringboot.mapper.UserMapper;
import com.itmao.aispringboot.util.EmotionAnalyzer;
import com.itmao.aispringboot.util.PrivacyMask;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConsultationSessionService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private ConsultationSessionMapper consultationSessionMapper;
    @Resource
    private ConsultationMessageService consultationMessageService;

    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("还没找到你的账号，先登录一下？");
        }
        ConsultationSession session = ConsultationSession.builder()
                .userId(userId)
                .sessionTitle(createDTO.getSessionTitle())
                .startedAt(LocalDateTime.now())
                .build();
        if (StrUtil.isBlank(createDTO.getSessionTitle())) {
            session.setSessionTitle("和小光的聊天 - " + DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm"));
        }
        consultationSessionMapper.insert(session);
        return session;
    }

    public Page<ConsultationSessionResponseDTO> pageSessions(Long currentUserId, Integer userType, PageQueryDTO query, boolean maskPrivateContent) {
        Page<ConsultationSession> page = new Page<>(query.resolveCurrent(), query.resolveSize());
        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        if (userType == null || userType != 2) {
            wrapper.eq(ConsultationSession::getUserId, currentUserId);
        }
        wrapper.orderByDesc(ConsultationSession::getStartedAt);
        Page<ConsultationSession> entityPage = consultationSessionMapper.selectPage(page, wrapper);
        Page<ConsultationSessionResponseDTO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toResponses(entityPage.getRecords(), maskPrivateContent));
        return result;
    }

    @Transactional
    public void deleteSession(Long sessionId, Long currentUserId, Integer userType) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("这段聊天找不到了");
        }
        if (userType == null || userType != 2) {
            if (!session.getUserId().equals(currentUserId)) {
                throw new BusinessException("这段聊天不属于你，没法删掉");
            }
        }
        consultationMessageService.deleteBySessionId(sessionId);
        consultationSessionMapper.deleteById(sessionId);
    }

    public Map<String, Object> getEmotion(Long sessionId) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("这段聊天找不到了");
        }
        if (StrUtil.isBlank(session.getLastEmotionAnalysis())) {
            ConsultationMessageResponseDTO last = consultationMessageService.getLastMessageBySessionId(sessionId);
            String json = EmotionAnalyzer.analyzeText(last != null ? last.getContent() : "", 5);
            session.setLastEmotionAnalysis(json);
            session.setLastEmotionUpdatedAt(LocalDateTime.now());
            consultationSessionMapper.updateById(session);
        }
        return EmotionAnalyzer.toMap(session.getLastEmotionAnalysis());
    }

    public void refreshEmotion(Long sessionId, String lastUserText) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setLastEmotionAnalysis(EmotionAnalyzer.analyzeText(lastUserText, null));
        session.setLastEmotionUpdatedAt(LocalDateTime.now());
        consultationSessionMapper.updateById(session);
    }

    public ConsultationSession getById(Long sessionId) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("这段聊天找不到了");
        }
        return session;
    }

    /**
     * 校验当前用户有权访问该会话（本人或管理员），否则抛出业务异常。
     */
    public ConsultationSession getAccessibleSession(Long sessionId, Long currentUserId, Integer userType) {
        ConsultationSession session = getById(sessionId);
        boolean isAdmin = userType != null && userType == 2;
        if (!isAdmin && !session.getUserId().equals(currentUserId)) {
            throw new BusinessException("这段聊天不属于你，没法查看");
        }
        return session;
    }

    public String getUserDisplayName(Long sessionId) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            return "";
        }
        User user = userMapper.selectById(session.getUserId());
        return user != null ? user.getDisplayName() : "";
    }

    private List<ConsultationSessionResponseDTO> toResponses(List<ConsultationSession> sessions, boolean maskPrivateContent) {
        if (sessions == null || sessions.isEmpty()) {
            return List.of();
        }
        List<Long> sessionIds = sessions.stream().map(ConsultationSession::getId).toList();
        List<Long> userIds = sessions.stream().map(ConsultationSession::getUserId).distinct().toList();
        Map<Long, User> users = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));
        Map<Long, Integer> counts = consultationMessageService.countBySessionIds(sessionIds);
        Map<Long, ConsultationMessage> lasts = consultationMessageService.lastBySessionIds(sessionIds);
        return sessions.stream()
                .map(session -> toResponse(session, users.get(session.getUserId()), lasts.get(session.getId()), counts.getOrDefault(session.getId(), 0), maskPrivateContent))
                .toList();
    }

    private ConsultationSessionResponseDTO toResponse(
            ConsultationSession session,
            User user,
            ConsultationMessage last,
            Integer count,
            boolean maskPrivateContent) {
        LocalDateTime lastTime = last != null ? last.getCreatedAt() : session.getStartedAt();
        long minutes = 0;
        if (session.getStartedAt() != null && lastTime != null) {
            minutes = Math.max(0, Duration.between(session.getStartedAt(), lastTime).toMinutes());
        }
        String lastContent = last != null ? last.getContent() : "";
        return ConsultationSessionResponseDTO.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .userNickname(user != null ? user.getDisplayName() : "")
                .sessionTitle(session.getSessionTitle())
                .startedAt(session.getStartedAt())
                .lastMessageContent(maskPrivateContent ? PrivacyMask.mask(lastContent) : lastContent)
                .lastMessageTime(lastTime)
                .messageCount(count)
                .durationMinutes(minutes)
                .lastEmotionAnalysis(session.getLastEmotionAnalysis())
                .contentMasked(maskPrivateContent)
                .build();
    }
}
