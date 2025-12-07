package com.classhub.domain.member.application;

import com.classhub.domain.auth.token.RefreshTokenStore;
import com.classhub.domain.member.dto.MemberSummary;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public Page<MemberSummary> getAssistants(
            UUID teacherId,
            String name,
            Boolean active,
            Pageable pageable
    ) {
        Member teacher = getTeacher(teacherId);

        Page<Member> page;
        if (Boolean.TRUE.equals(active) && name != null && !name.isBlank()) {
            page = memberRepository.findAllByRoleAndTeacherIdAndIsActiveAndNameContainingIgnoreCase(
                    MemberRole.ASSISTANT,
                    teacher.getId(),
                    true,
                    name,
                    pageable
            );
        } else if (Boolean.TRUE.equals(active)) {
            page = memberRepository.findAllByRoleAndTeacherIdAndIsActive(
                    MemberRole.ASSISTANT,
                    teacher.getId(),
                    true,
                    pageable
            );
        } else if (name != null && !name.isBlank()) {
            // active null 또는 false 포함 조회
            if (active != null) {
                page = memberRepository.findAllByRoleAndTeacherIdAndIsActiveAndNameContainingIgnoreCase(
                        MemberRole.ASSISTANT,
                        teacher.getId(),
                        active,
                        name,
                        pageable
                );
            } else {
                page = memberRepository.findAllByRoleAndTeacherIdAndNameContainingIgnoreCase(
                        MemberRole.ASSISTANT,
                        teacher.getId(),
                        name,
                        pageable
                );
            }
        } else if (active != null) {
            page = memberRepository.findAllByRoleAndTeacherIdAndIsActive(
                    MemberRole.ASSISTANT,
                    teacher.getId(),
                    active,
                    pageable
            );
        } else {
            page = memberRepository.findAllByRoleAndTeacherId(
                    MemberRole.ASSISTANT,
                    teacher.getId(),
                    pageable
            );
        }

        return page.map(MemberSummary::from);
    }

    @Transactional
    public void deactivateAssistant(UUID teacherId, UUID assistantId) {
        Member teacher = getTeacher(teacherId);
        Member assistant = memberRepository.findById(assistantId)
                .orElseThrow(() -> new BusinessException(RsCode.ASSISTANT_NOT_FOUND));

        if (assistant.getRole() != MemberRole.ASSISTANT || !teacher.getId().equals(assistant.getTeacherId())) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }

        assistant.deactivate();
        memberRepository.save(assistant);
        refreshTokenStore.blacklistAllForMember(assistant.getId());
    }

    private Member getTeacher(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));
        if (member.getRole() != MemberRole.TEACHER) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
        return member;
    }
}
