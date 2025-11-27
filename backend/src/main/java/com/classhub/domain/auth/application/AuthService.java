package com.classhub.domain.auth.application;

import com.classhub.domain.auth.dto.TeacherRegisterRequest;
import com.classhub.domain.auth.dto.TeacherRegisterResponse;
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
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TeacherRegisterResponse registerTeacher(TeacherRegisterRequest request) {
        String email = request.normalizedEmail();
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(RsCode.DUPLICATE_EMAIL);
        }

        Member teacher = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.sanitizedName())
                .role(MemberRole.TEACHER)
                .build();

        Member saved = memberRepository.save(teacher);
        return TeacherRegisterResponse.from(saved);
    }
}
