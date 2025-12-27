package com.classhub.domain.member.application;

import com.classhub.domain.member.dto.request.MemberProfileUpdateRequest;
import com.classhub.domain.member.dto.request.StudentInfoUpdateRequest;
import com.classhub.domain.member.dto.response.MemberProfileResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.UUID;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));

        StudentInfo info = null;
        if (member.getRole() == MemberRole.STUDENT) {
            info = studentInfoRepository.findByMemberId(memberId)
                    .orElseThrow(() -> new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND));
        }
        return MemberProfileResponse.from(member, info);
    }

    @Transactional
    public MemberProfileResponse updateProfile(UUID memberId, MemberProfileUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));

        updateEmail(member, request);

        member.changeName(request.trimmedName());
        member.changePhoneNumber(request.normalizedPhoneNumber());

        if (request.password() != null) {
            member.changePassword(passwordEncoder.encode(request.password()));
        }

        StudentInfo info = null;
        if (request.studentInfo() != null) {
            if (member.getRole() != MemberRole.STUDENT) {
                throw new BusinessException(RsCode.BAD_REQUEST);
            }
            info = updateStudentInfo(memberId, request.studentInfo());
        } else if (member.getRole() == MemberRole.STUDENT) {
            info = studentInfoRepository.findByMemberId(memberId)
                    .orElseThrow(() -> new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND));
        }

        return MemberProfileResponse.from(member, info);
    }

    private void updateEmail(Member member, MemberProfileUpdateRequest request) {
        String normalizedEmail = request.normalizedEmail();
        if (normalizedEmail == null || Objects.equals(member.getEmail(), normalizedEmail)) {
            return;
        }

        memberRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            if (existing.getId().equals(member.getId())) {
                return;
            }
            if (existing.isDeleted()) {
                throw new BusinessException(RsCode.MEMBER_INACTIVE);
            }
            throw new BusinessException(RsCode.DUPLICATE_EMAIL);
        });
        member.changeEmail(normalizedEmail);
    }

    private StudentInfo updateStudentInfo(UUID memberId, StudentInfoUpdateRequest request) {
        StudentInfo info = studentInfoRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(RsCode.STUDENT_PROFILE_NOT_FOUND));
        info.updateSchoolName(request.formattedSchoolName());
        info.updateGrade(request.grade());
        info.updateBirthDate(request.birthDate());
        info.updateParentPhone(request.normalizedParentPhone());
        return info;
    }
}
