package com.itmao.aispringboot.util;

import com.itmao.aispringboot.exception.BusinessException;

public final class ModelCallErrors {
    public static final String USER_KEY_INVALID =
            "你填的 API Key 好像无效或额度用完了。去「我的」里检查一下？";
    public static final String PLATFORM_KEY_UNAVAILABLE =
            "小光这边的模型暂时连不上。你的免费次数还在，可以去「我的」里填自己的 Key。";
    public static final String SIGNAL_BAD = "刚才信号不好，你再说一遍也没关系。";

    private ModelCallErrors() {
    }

    public static RuntimeException wrap(boolean hasOwnApiKey, Throwable error) {
        if (error instanceof BusinessException) {
            return (BusinessException) error;
        }
        return new BusinessException(userMessage(hasOwnApiKey, error));
    }

    public static String userMessage(boolean hasOwnApiKey, Throwable error) {
        if (!isAuthFailure(error)) {
            return SIGNAL_BAD;
        }
        return hasOwnApiKey ? USER_KEY_INVALID : PLATFORM_KEY_UNAVAILABLE;
    }

    public static boolean isAuthFailure(Throwable error) {
        Throwable cause = root(error);
        String raw = ((cause.getMessage() == null ? "" : cause.getMessage())
                + " " + cause.getClass().getSimpleName()).toLowerCase();
        return raw.contains("authentication fails")
                || raw.contains("unauthorized")
                || raw.contains("invalid api key")
                || raw.contains("incorrect api key")
                || raw.contains("invalid_api_key")
                || (raw.contains("api key") && (raw.contains("401") || raw.contains("invalid")));
    }

    public static String streamErrorMessage(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof BusinessException business) {
                return business.getMessage();
            }
            current = current.getCause();
        }
        return userMessage(false, ex);
    }

    private static Throwable root(Throwable error) {
        Throwable cause = error;
        while (cause != null && cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause == null ? error : cause;
    }
}
