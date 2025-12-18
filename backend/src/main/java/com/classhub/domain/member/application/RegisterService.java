package com.classhub.domain.member.application;

import com.classhub.domain.auth.application.AuthService;
import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.member.dto.request.RegisterMemberRequest;
import com.classhub.domain.member.dto.request.RegisterStudentRequest;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
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
    private final StudentInfoRepository studentInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional
    public AuthTokens registerTeacher(RegisterMemberRequest request) {
        String normalizedEmail = request.normalizedEmail();
        ensureEmailAvailable(normalizedEmail);

        Member member = createMember(request, MemberRole.TEACHER);

        return authService.login(new LoginRequest(normalizedEmail, request.password()));
    }

    @Transactional
    public AuthTokens registerStudent(RegisterStudentRequest request) {
        RegisterMemberRequest memberRequest = request.memberRequest();
        String normalizedEmail = memberRequest.normalizedEmail();
        ensureEmailAvailable(normalizedEmail);

        Member member = createMember(memberRequest, MemberRole.STUDENT);

        StudentInfo studentInfo = StudentInfo.create(
                member,
                request.formattedSchoolName(),
                request.grade(),
                request.birthDate(),
                request.normalizedParentPhone()
        );
        studentInfoRepository.save(studentInfo);

        return authService.login(new LoginRequest(normalizedEmail, memberRequest.password()));
    }

    private void ensureEmailAvailable(String normalizedEmail) {
        memberRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            if (existing.isDeleted()) {
                throw new BusinessException(RsCode.MEMBER_INACTIVE);
            }
            throw new BusinessException(RsCode.DUPLICATE_EMAIL);
        });
    }

    private Member createMember(RegisterMemberRequest request, MemberRole role) {
        Member member = Member.builder()
                .email(request.normalizedEmail())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .phoneNumber(request.normalizedPhoneNumber())
                .role(role)
                .build();
        return memberRepository.save(member);
    }
}
