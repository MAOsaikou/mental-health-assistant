package com.itmao.aispringboot.config;

import com.itmao.aispringboot.common.ResultCode;
import com.itmao.aispringboot.util.ResponseUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    private static final String[] PUBLIC_PATHS = {
            "/",
            "/api/user/login",
            "/api/user/add",
            "/api/user/refresh",
            "/files/bussiness/user_avatar/**",
            "/files/bussiness/article/**"
    };

    public static boolean isPublicPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        for (String publicPath : PUBLIC_PATHS) {
            if (ANT_PATH_MATCHER.match(publicPath, uri)) {
                return true;
            }
        }
        if ("GET".equalsIgnoreCase(method)
                && (ANT_PATH_MATCHER.match("/api/knowledge/article/**", uri)
                || ANT_PATH_MATCHER.match("/api/knowledge/category/tree", uri))) {
            return true;
        }
        return false;
    }

    public static boolean isPublicPath(String requestUri) {
        for (String publicPath : PUBLIC_PATHS) {
            if (ANT_PATH_MATCHER.match(publicPath, requestUri)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SSE/Flux 会走 ASYNC 二次调度；JWT 过滤器默认不重复执行，必须放行否则 403
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/knowledge/article/**", "/api/knowledge/category/tree").permitAll()
                        // 管理端接口仅管理员可访问
                        .requestMatchers("/api/data-analytics/**").hasRole("ADMIN")
                        .requestMatchers("/api/emotion-diary/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/knowledge/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/knowledge/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/knowledge/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) ->
                                ResponseUtil.writeError(response, ResultCode.AUTHORIZED_ERROR))
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
