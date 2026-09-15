package com.itmao.aispringboot.controller;

import cn.hutool.json.JSONUtil;
import com.itmao.aispringboot.AiService.PsychologicalSupportService;
import com.itmao.aispringboot.AiService.StructOutPut;
import com.itmao.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.itmao.aispringboot.DTO.command.ConsultationStreamDTO;
import com.itmao.aispringboot.DTO.query.PageQueryDTO;
import com.itmao.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.itmao.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import com.itmao.aispringboot.common.Result;
import com.itmao.aispringboot.common.ResultCode;
import com.itmao.aispringboot.service.ConsultationMessageService;
import com.itmao.aispringboot.service.ConsultationSessionService;
import com.itmao.aispringboot.util.JwtTokenUtil;
import com.itmao.aispringboot.util.RequestRateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChat {

    private final PsychologicalSupportService psychologicalSupportService;
    private final ConsultationSessionService consultationSessionService;
    private final ConsultationMessageService consultationMessageService;
    private final RequestRateLimiter requestRateLimiter;

    public PsychologicalChat(PsychologicalSupportService psychologicalSupportService,
                             ConsultationSessionService consultationSessionService,
                             ConsultationMessageService consultationMessageService,
                             RequestRateLimiter requestRateLimiter) {
        this.psychologicalSupportService = psychologicalSupportService;
        this.consultationSessionService = consultationSessionService;
        this.consultationMessageService = consultationMessageService;
        this.requestRateLimiter = requestRateLimiter;
    }

    @PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        requestRateLimiter.assertAllowed("chat-start:" + userId, 10, Duration.ofMinutes(10));
        return Result.success(psychologicalSupportService.startSession(userId, createDTO));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        if (userId == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg(), "用户未登录")))
                    .build());
        }
        requestRateLimiter.assertAllowed("chat-stream:" + userId, 30, Duration.ofMinutes(10));
        return psychologicalSupportService.streamPsychologicalChat(streamDTO.getSessionId(), streamDTO.getUserMessage(), userId)
                .map(fragment -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(JSONUtil.toJsonStr(Result.success(Map.of("content", fragment, "type", "normal"))))
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()))
                .delayElements(Duration.ofMillis(50))
                .onErrorResume(ex -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(JSONUtil.toJsonStr(Result.error(
                                ResultCode.ERROR.getCode(),
                                streamErrorMessage(ex),
                                null)))
                        .build()));
    }

    private static String streamErrorMessage(Throwable ex) {
        return com.itmao.aispringboot.util.ModelCallErrors.streamErrorMessage(ex);
    }

    @GetMapping("/sessions")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConsultationSessionResponseDTO>> sessions(
            PageQueryDTO query,
            @RequestParam(defaultValue = "false") boolean reveal) {
        boolean mask = JwtTokenUtil.isCurrentAdmin() && !reveal;
        return Result.success(consultationSessionService.pageSessions(
                JwtTokenUtil.getCurrentUserId(),
                JwtTokenUtil.getCurrentRoleType(),
                query,
                mask
        ));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> delete(@PathVariable Long sessionId) {
        consultationSessionService.deleteSession(sessionId, JwtTokenUtil.getCurrentUserId(), JwtTokenUtil.getCurrentRoleType());
        return Result.success();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessageResponseDTO>> messages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "false") boolean reveal) {
        consultationSessionService.getAccessibleSession(sessionId,
                JwtTokenUtil.getCurrentUserId(), JwtTokenUtil.getCurrentRoleType());
        boolean mask = JwtTokenUtil.isCurrentAdmin() && !reveal;
        return Result.success(consultationMessageService.listBySessionId(sessionId, mask));
    }

    @GetMapping("/session/{sessionId}/emotion")
    public Result<Map<String, Object>> emotion(@PathVariable String sessionId) {
        Long id = psychologicalSupportService.extractSessionId(sessionId);
        consultationSessionService.getAccessibleSession(id,
                JwtTokenUtil.getCurrentUserId(), JwtTokenUtil.getCurrentRoleType());
        return Result.success(consultationSessionService.getEmotion(id));
    }
}
