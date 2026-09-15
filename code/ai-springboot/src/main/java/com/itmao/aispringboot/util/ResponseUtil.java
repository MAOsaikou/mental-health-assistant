package com.itmao.aispringboot.util;

import cn.hutool.json.JSONUtil;
import com.itmao.aispringboot.common.Result;
import com.itmao.aispringboot.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class ResponseUtil {

    public static void writeError(HttpServletResponse response, ResultCode resultCode) {
        int status = switch (resultCode) {
            case UNAUTHORIZED, ACCESS_UNAUTHORIZED, TOKEN_INVALID, TOKEN_EXPIRED, TOKEN_BLOCKED
                    -> HttpStatus.UNAUTHORIZED.value();
            case TOKEN_ACCESS_FORBIDDEN, AUTHORIZED_ERROR -> HttpStatus.FORBIDDEN.value();
            default -> HttpStatus.BAD_REQUEST.value();
        };
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter writer = response.getWriter()) {
            writer.print(JSONUtil.toJsonStr(Result.error(resultCode.getCode(), resultCode.getMsg(), null)));
            writer.flush();
        } catch (IOException e) {
            System.out.println("写入响应失败：" + e.getMessage());
        }
    }
}
