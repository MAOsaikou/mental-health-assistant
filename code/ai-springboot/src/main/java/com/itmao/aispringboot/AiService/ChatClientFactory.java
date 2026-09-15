package com.itmao.aispringboot.AiService;

import cn.hutool.core.util.StrUtil;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.util.ApiKeyCrypto;
import com.itmao.aispringboot.util.ModelEndpointGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatClientFactory {
    private static final Logger log = LoggerFactory.getLogger(ChatClientFactory.class);
    private static final String UNCONFIGURED_PLATFORM_KEY = "not-configured";

    private final ChatClient defaultClient;
    private final ChatMemory chatMemory;
    private final ApiKeyCrypto apiKeyCrypto;
    private final ModelEndpointGuard modelEndpointGuard;
    private final String defaultBaseUrl;
    private final String defaultModel;
    private final boolean platformKeyConfigured;

    public ChatClientFactory(
            @Qualifier("open-ai") ChatClient defaultClient,
            ChatMemory chatMemory,
            ApiKeyCrypto apiKeyCrypto,
            ModelEndpointGuard modelEndpointGuard,
            @Value("${spring.ai.openai.api-key:}") String platformApiKey,
            @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String defaultBaseUrl,
            @Value("${spring.ai.openai.chat.options.model:deepseek-chat}") String defaultModel) {
        this.defaultClient = defaultClient;
        this.chatMemory = chatMemory;
        this.apiKeyCrypto = apiKeyCrypto;
        this.modelEndpointGuard = modelEndpointGuard;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
        this.platformKeyConfigured = StrUtil.isNotBlank(platformApiKey)
                && !UNCONFIGURED_PLATFORM_KEY.equals(platformApiKey);
        if (this.platformKeyConfigured) {
            log.info("已加载平台 API Key，未填写自己 Key 的用户可使用免费次数");
        } else {
            log.warn("未配置平台 API Key，免费次数不可用。请通过部署平台 Secret/DEEPSEEK_API_KEY 环境变量注入，或让用户在「我的」里填写自己的 Key");
        }
    }

    public ChatClient forUser(User user) {
        if (user != null && user.hasOwnApiKey()) {
            String baseUrl = modelEndpointGuard.sanitizeBaseUrl(
                    StrUtil.blankToDefault(user.getApiBaseUrl(), defaultBaseUrl));
            String model = StrUtil.blankToDefault(user.getApiModel(), defaultModel);
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .options(OpenAiChatOptions.builder()
                            .apiKey(apiKeyCrypto.decrypt(user.getApiKey()).trim())
                            .baseUrl(baseUrl)
                            .model(model)
                            .build())
                    .build();
            return ChatClient.builder(chatModel)
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                    .defaultSystem("你是小光，小光心理助手里的陪伴者。短句、先听、不做诊断，全程简体中文。")
                    .build();
        }
        if (!platformKeyConfigured) {
            throw new BusinessException(
                    "还没有可用的模型密钥。去「我的」里填上你自己的 API Key，小光就能陪你。");
        }
        return defaultClient;
    }
}
