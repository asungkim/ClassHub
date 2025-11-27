package com.classhub.global.exception.jwt;

import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsConstant;
import com.classhub.global.response.RsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;

import static com.classhub.global.response.RsCode.UNAUTHENTICATED;

/**
 * 스프링 시큐리티 필터에서 인증 실패 시 예외 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws
        IOException {
        writeUnauthorizedResponse(response, e);
    }

    public void commence(HttpServletRequest request, HttpServletResponse response, BusinessException e) throws
        IOException {
        writeUnauthorizedResponse(response, e);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, Exception e) throws IOException {
        log.error("[AuthenticationException] ex", e);

        response.setStatus(RsConstant.UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String json = objectMapper.writeValueAsString(RsData.from(UNAUTHENTICATED));

        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
            writer.flush(); // WAS가 클라이언트에게 즉시 응답
        }
    }
}
