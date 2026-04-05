package org.kwakmunsu.haruhana.global.security.jwt;

import static org.kwakmunsu.haruhana.global.security.jwt.enums.TokenType.AUTHORIZATION_HEADER;
import static org.kwakmunsu.haruhana.global.security.jwt.enums.TokenType.BEARER_PREFIX;
import static org.kwakmunsu.haruhana.global.support.error.ErrorType.EMPTY_TOKEN;
import static org.kwakmunsu.haruhana.global.support.error.ErrorType.INVALID_TOKEN;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.kwakmunsu.haruhana.global.support.error.ErrorType;
import org.kwakmunsu.haruhana.global.support.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return Arrays.stream(SecurityPaths.PERMIT_ALL)
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path))
                || Arrays.asList(SecurityPaths.ACTUATOR_PERMIT).contains(path);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<String> tokenOpt = getTokenFromHeader(request);

        // 토큰이 없는 경우
        if (tokenOpt.isEmpty()) {
            sendErrorResponse(response, EMPTY_TOKEN);
            return;
        }

        // 토큰이 존재하지만 유효하지 않은 경우
        String token = tokenOpt.get();
        if (!jwtProvider.isTokenValid(token)) {
            sendErrorResponse(response, INVALID_TOKEN);
            return;
        }

        Authentication authentication = jwtProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private Optional<String> getTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER.getValue());

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX.getValue())) {
            String token = bearerToken.substring(BEARER_PREFIX.getValue().length());

            return Optional.of(token);
        }

        return Optional.empty();
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType) throws IOException {
        log.warn("[Auth] Jwt 인증 처리 중 예외 발생: {}", errorType.getMessage());

        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(errorType.getStatus().value());

        String json = objectMapper.writeValueAsString(ApiResponse.error(errorType));
        response.getWriter().write(json);
    }

}