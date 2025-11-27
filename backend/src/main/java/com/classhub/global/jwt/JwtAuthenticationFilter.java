package com.classhub.global.jwt;

import com.classhub.global.exception.BusinessException;
import com.classhub.global.exception.jwt.JwtAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.springframework.util.StringUtils;

import static com.classhub.global.response.RsCode.UNAUTHENTICATED;

/**
 * 토큰 검증 및 사용자 인증 정보를 저장하는 필터
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final static String HEADER_AUTHORIZATION = "Authorization";
    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;


    /**
     * 스프링 시큐리티 필터에서 인증 처리
     **/
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);
        String accessToken = jwtProvider.getTokenFromHeader(authorizationHeader);

        if (StringUtils.hasText(accessToken)) {
            if (!jwtProvider.isValidToken(accessToken)) {
                jwtAuthenticationEntryPoint.commence(request, response, new BusinessException(UNAUTHENTICATED));
                return;
            }
            Authentication authentication = jwtProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
