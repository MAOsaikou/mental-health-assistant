package com.itmao.aispringboot.config;

import com.itmao.aispringboot.common.ResultCode;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.enumClass.UserStatus;
import com.itmao.aispringboot.service.UserService;
import com.itmao.aispringboot.util.JwtTokenUtil;
import com.itmao.aispringboot.util.ResponseUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private UserService userService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String token = JwtTokenUtil.extractTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            clearSecurityContext();
            if (SecurityConfig.isPublicPath(request)) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }

        JwtTokenUtil.TokenVerificationResult validationResult = JwtTokenUtil.validateToken(token);
        if (validationResult == null || !validationResult.isValid()) {
            clearSecurityContext();
            if (SecurityConfig.isPublicPath(request)) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
            return;
        }

        User user;
        try {
            user = userService.getEntity(validationResult.getUserId());
        } catch (Exception e) {
            clearSecurityContext();
            if (SecurityConfig.isPublicPath(request)) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
            return;
        }
        if (user == null || !UserStatus.NORMAL.getCode().equals(user.getStatus())) {
            clearSecurityContext();
            if (SecurityConfig.isPublicPath(request)) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
            return;
        }
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (validationResult.getTokenVersion() == null
                || currentVersion != validationResult.getTokenVersion()) {
            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.TOKEN_BLOCKED);
            return;
        }

        Integer roleType = user.getUserType() == null ? 1 : user.getUserType();
        String role = Integer.valueOf(2).equals(roleType) ? "ROLE_ADMIN" : "ROLE_USER";
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(role)
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("jwtToken", token);
        request.setAttribute("currentUserId", user.getId());
        request.setAttribute("currentRoleType", roleType);

        chain.doFilter(request, response);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
