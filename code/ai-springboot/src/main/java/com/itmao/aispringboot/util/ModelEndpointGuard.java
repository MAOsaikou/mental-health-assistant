package com.itmao.aispringboot.util;

import cn.hutool.core.util.StrUtil;
import com.itmao.aispringboot.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ModelEndpointGuard {

    private final Set<String> allowedHosts;
    private final String defaultBaseUrl;

    public ModelEndpointGuard(
            @Value("${app.ai.allowed-base-hosts:api.deepseek.com,api.openai.com,dashscope.aliyuncs.com,api.moonshot.cn,open.bigmodel.cn,api.siliconflow.cn}")
            String allowedBaseHosts,
            @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String defaultBaseUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
        Set<String> hosts = Arrays.stream(allowedBaseHosts.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(host -> host.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String defaultHost = hostOf(defaultBaseUrl);
        if (defaultHost != null) {
            hosts.add(defaultHost);
        }
        this.allowedHosts = Set.copyOf(hosts);
    }

    public String sanitizeBaseUrl(String raw) {
        if (StrUtil.isBlank(raw)) {
            return defaultBaseUrl;
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("接口地址格式不太对，请填 https://api.deepseek.com 这样的地址");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BusinessException("接口地址只支持 https");
        }
        if (uri.getUserInfo() != null) {
            throw new BusinessException("接口地址不合法");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BusinessException("接口地址缺少域名");
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (isIpAddress(host) || isLocalHost(host)) {
            throw new BusinessException("接口地址不能使用 IP 或本机地址");
        }
        if (uri.getPort() != -1 && uri.getPort() != 443) {
            throw new BusinessException("接口地址只支持 https 默认端口");
        }
        if (!allowedHosts.contains(host)) {
            throw new BusinessException("这个接口地址还不支持。请用 DeepSeek、OpenAI 等常见地址");
        }
        return "https://" + host;
    }

    private static String hostOf(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        try {
            String host = URI.create(url.trim()).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isLocalHost(String host) {
        return "localhost".equals(host) || host.endsWith(".localhost") || host.endsWith(".local");
    }

    private static boolean isIpAddress(String host) {
        if (host.contains(":")) {
            return true;
        }
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
                return false;
            }
            int value = Integer.parseInt(part);
            if (value > 255) {
                return false;
            }
        }
        return true;
    }
}
