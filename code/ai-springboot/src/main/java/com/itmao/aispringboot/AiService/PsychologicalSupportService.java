package com.itmao.aispringboot.AiService;

import com.itmao.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.itmao.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.itmao.aispringboot.entity.ConsultationSession;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.service.ConsultationMessageService;
import com.itmao.aispringboot.service.ConsultationSessionService;
import com.itmao.aispringboot.service.UserService;
import com.itmao.aispringboot.util.CrisisSupport;
import com.itmao.aispringboot.util.ModelCallErrors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class PsychologicalSupportService {
    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    @Autowired
    private UserService userService;

    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        User user = userService.getEntity(userId);
        userService.assertCanUsePlatformChat(user);
        ConsultationSession consultationSession = consultationSessionService.createSession(userId, createDTO);
        consultationMessageService.saveUserMessage(consultationSession.getId(), createDTO.getInitialMessage(), null);
        String sessionId = "session_" + consultationSession.getId();
        return new StructOutPut.StreamChatSession(
                sessionId,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L,
                1,
                "ACTIVE"
        );
    }

    public Flux<String> streamPsychologicalChat(String sessionId, String userMessage, Long currentUserId) {
        return Flux.create(sink -> {
            Long dbSessionId = extractSessionId(sessionId);
            if (dbSessionId == null) {
                sink.error(new BusinessException("会话ID格式错误"));
                return;
            }
            ConsultationSession session = consultationSessionService.getById(dbSessionId);
            // 只允许会话本人发消息，防止越权使用他人会话
            if (currentUserId == null || !session.getUserId().equals(currentUserId)) {
                sink.error(new BusinessException("这段聊天不属于你"));
                return;
            }
            User user = userService.getEntity(session.getUserId());

            boolean isInitialMessage = false;
            Integer messageCount = consultationMessageService.getMessageCountBySessionId(dbSessionId);
            if (messageCount == 1) {
                ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
                if (lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())) {
                    isInitialMessage = true;
                }
            }

            boolean crisis = CrisisSupport.isCrisis(userMessage);
            if (!crisis && !userService.canUsePlatformChat(user, !isInitialMessage)) {
                if (!isInitialMessage) {
                    // 额度用尽时不占用新的对话次数
                    String reply = userService.quotaExceededMessage();
                    for (String chunk : CrisisSupport.splitChunks(reply, 16)) {
                        sink.next(chunk);
                    }
                    sink.complete();
                    return;
                }
                String reply = userService.quotaExceededMessage();
                for (String chunk : CrisisSupport.splitChunks(reply, 16)) {
                    sink.next(chunk);
                }
                consultationMessageService.saveAimessage(dbSessionId, reply, "xiaoguang");
                sink.complete();
                return;
            }

            if (!isInitialMessage) {
                consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
            }

            if (crisis) {
                String reply = CrisisSupport.FIXED_REPLY;
                for (String chunk : CrisisSupport.splitChunks(reply, 16)) {
                    sink.next(chunk);
                }
                consultationMessageService.saveAimessage(dbSessionId, reply, "xiaoguang");
                consultationSessionService.refreshEmotion(dbSessionId, userMessage);
                sink.complete();
                return;
            }

            String conversationId = "conversation_" + sessionId;
            hydrateMemoryFromDatabase(conversationId, dbSessionId, userMessage);

            String nickname = user.getDisplayName();
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(PromptManage.buildSystemPrompt(nickname))
            ));

            ChatClient chatClient = chatClientFactory.forUser(user);
            String modelName = user.hasOwnApiKey() && user.getApiModel() != null && !user.getApiModel().isBlank()
                    ? user.getApiModel()
                    : "deepseek";
            StringBuilder fullResponse = new StringBuilder();
            chatClient.prompt(prompt)
                    .user(userMessage)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .doOnNext(fragment -> {
                        fullResponse.append(fragment);
                        sink.next(fragment);
                    })
                    .onErrorMap(error -> ModelCallErrors.wrap(user.hasOwnApiKey(), error))
                    .subscribe(
                            ignored -> {
                            },
                            sink::error,
                            () -> {
                                String completeRes = fullResponse.toString();
                                if (!completeRes.isBlank()) {
                                    consultationMessageService.saveAimessage(dbSessionId, completeRes, modelName);
                                    consultationSessionService.refreshEmotion(dbSessionId, userMessage);
                                }
                                sink.complete();
                            }
                    );
        });
    }

    private void hydrateMemoryFromDatabase(String conversationId, Long dbSessionId, String currentUserMessage) {
        chatMemory.clear(conversationId);
        List<ConsultationMessageResponseDTO> stored = consultationMessageService.listBySessionId(dbSessionId);
        int end = stored.size();
        if (end > 0) {
            ConsultationMessageResponseDTO last = stored.get(end - 1);
            if (Integer.valueOf(1).equals(last.getSenderType())
                    && currentUserMessage != null
                    && currentUserMessage.equals(last.getContent())) {
                end -= 1;
            }
        }
        int start = Math.max(0, end - 29);
        List<Message> history = new ArrayList<>();
        for (int i = start; i < end; i++) {
            ConsultationMessageResponseDTO msg = stored.get(i);
            String content = msg.getContent() == null ? "" : msg.getContent();
            if (Integer.valueOf(1).equals(msg.getSenderType())) {
                history.add(new UserMessage(content));
            } else if (Integer.valueOf(2).equals(msg.getSenderType())) {
                history.add(new AssistantMessage(content));
            }
        }
        if (!history.isEmpty()) {
            chatMemory.add(conversationId, history);
        }
    }

    public Long extractSessionId(String sessionId) {
        if (sessionId != null && sessionId.startsWith("session_")) {
            return Long.parseLong(sessionId.substring("session_".length()));
        }
        if (sessionId != null) {
            try {
                return Long.parseLong(sessionId);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
