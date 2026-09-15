package com.itmao.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itmao.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.itmao.aispringboot.DTO.response.SessionMessageCountDTO;
import com.itmao.aispringboot.entity.ConsultationMessage;
import com.itmao.aispringboot.mapper.ConsultationMessageMapper;
import com.itmao.aispringboot.util.PrivacyMask;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConsultationMessageService {
    @Resource
    private ConsultationMessageMapper consultationMessageMapper;

    public ConsultationMessage saveUserMessage(Long sessionId, String content, String emotionTag) {
        ConsultationMessage userMessage = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(1)
                .messageType(1)
                .content(content)
                .emotionTag(emotionTag)
                .createdAt(LocalDateTime.now())
                .build();
        consultationMessageMapper.insert(userMessage);
        return userMessage;
    }

    public ConsultationMessage saveAimessage(Long sessionId, String content, String aiModel) {
        ConsultationMessage message = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(2)
                .messageType(1)
                .content(content)
                .aiModel(aiModel)
                .createdAt(LocalDateTime.now())
                .build();
        consultationMessageMapper.insert(message);
        return message;
    }

    public Integer getMessageCountBySessionId(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId);
        return consultationMessageMapper.selectCount(queryWrapper).intValue();
    }

    public int countUserTurns(Long userId) {
        if (userId == null) {
            return 0;
        }
        return consultationMessageMapper.countUserTurns(userId);
    }

    public void deleteBySessionId(Long sessionId) {
        consultationMessageMapper.delete(new LambdaQueryWrapper<ConsultationMessage>()
                .eq(ConsultationMessage::getSessionId, sessionId));
    }

    public ConsultationMessageResponseDTO getLastMessageBySessionId(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId)
                .orderByDesc(ConsultationMessage::getCreatedAt)
                .last("limit 1");
        ConsultationMessage lastMessage = consultationMessageMapper.selectOne(queryWrapper);
        return lastMessage != null ? convertToResponseDTO(lastMessage) : null;
    }

    public Map<Long, Integer> countBySessionIds(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return consultationMessageMapper.countBySessionIds(sessionIds).stream()
                .filter(item -> item.getSessionId() != null)
                .collect(Collectors.toMap(
                        SessionMessageCountDTO::getSessionId,
                        item -> item.getMessageCount() == null ? 0 : item.getMessageCount(),
                        (left, right) -> left
                ));
    }

    public Map<Long, ConsultationMessage> lastBySessionIds(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return consultationMessageMapper.findLastBySessionIds(sessionIds).stream()
                .filter(item -> item.getSessionId() != null)
                .collect(Collectors.toMap(ConsultationMessage::getSessionId, item -> item, (left, right) -> left));
    }

    public List<ConsultationMessageResponseDTO> listBySessionId(Long sessionId) {
        return listBySessionId(sessionId, false);
    }

    public List<ConsultationMessageResponseDTO> listBySessionId(Long sessionId, boolean maskContent) {
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId)
                .orderByAsc(ConsultationMessage::getCreatedAt);
        return consultationMessageMapper.selectList(queryWrapper).stream()
                .map(message -> convertToResponseDTO(message, maskContent))
                .toList();
    }

    public ConsultationMessageResponseDTO convertToResponseDTO(ConsultationMessage message) {
        return convertToResponseDTO(message, false);
    }

    public ConsultationMessageResponseDTO convertToResponseDTO(ConsultationMessage message, boolean maskContent) {
        if (message == null) {
            return null;
        }
        ConsultationMessageResponseDTO responseDTO = new ConsultationMessageResponseDTO();
        responseDTO.setId(message.getId());
        responseDTO.setSessionId(message.getSessionId());
        responseDTO.setSenderType(message.getSenderType());
        responseDTO.setMessageType(message.getMessageType());
        String content = message.getContent();
        responseDTO.setContent(maskContent ? PrivacyMask.mask(content) : content);
        responseDTO.setEmotionTag(message.getEmotionTag());
        responseDTO.setAiModel(message.getAiModel());
        responseDTO.setCreatedAt(message.getCreatedAt());
        responseDTO.setSenderTypeDesc(message.getSenderTypeDesc());
        responseDTO.setMessageTypeDesc(message.getMessageTypeDesc());
        responseDTO.setContentMasked(maskContent);
        responseDTO.calculateContentLength();
        return responseDTO;
    }
}
