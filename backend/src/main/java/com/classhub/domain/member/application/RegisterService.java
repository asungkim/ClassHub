package com.classhub.domain.member.application;

import com.classhub.domain.auth.application.AuthService;
import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.member.dto.request.RegisterTeacherRequest;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional
    public AuthTokens registerTeacher(RegisterTeacherRequest request) {
        String normalizedEmail = request.normalizedEmail();
        memberRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            if (existing.isDeleted()) {
                throw new BusinessException(RsCode.MEMBER_INACTIVE);
            }
            throw new BusinessException(RsCode.DUPLICATE_EMAIL);
        });

        Member member = Member.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .phoneNumber(request.normalizedPhoneNumber())
                .role(MemberRole.TEACHER)
                .build();
        memberRepository.save(member);

        return authService.login(new LoginRequest(normalizedEmail, request.password()));
    }
}
