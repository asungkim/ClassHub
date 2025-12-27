package com.classhub.global.init.data;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile({"local", "test"})
public class TeacherAssistantAssignmentInitData extends BaseInitData {

    private static final List<String> TEACHER_EMAILS = List.of("te1@n.com", "te2@n.com");
    private static final List<String> ASSISTANT_EMAILS = List.of("as1@n.com", "as2@n.com", "as3@n.com", "as4@n.com");

    private final TeacherAssistantAssignmentRepository assignmentRepository;
    private final MemberRepository memberRepository;

    public TeacherAssistantAssignmentInitData(TeacherAssistantAssignmentRepository assignmentRepository,
                                              MemberRepository memberRepository) {
        super("season2-teacher-assistant-assignment-seed", 50);
        this.assignmentRepository = assignmentRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        List<Member> teachers = loadMembers(TEACHER_EMAILS, MemberRole.TEACHER);
        List<Member> assistants = loadMembers(ASSISTANT_EMAILS, MemberRole.ASSISTANT);

        if (teachers.isEmpty() || assistants.isEmpty()) {
            log.warn("Skipping teacher-assistant seed. teachers={}, assistants={}", teachers.size(), assistants.size());
            return;
        }

        for (Member teacher : teachers) {
            for (Member assistant : assistants) {
                upsertAssignment(teacher.getId(), assistant.getId(), force);
            }
        }
    }

    private void upsertAssignment(UUID teacherId, UUID assistantId, boolean force) {
        Optional<TeacherAssistantAssignment> existing = assignmentRepository
                .findByTeacherMemberIdAndAssistantMemberId(teacherId, assistantId);
        if (existing.isPresent()) {
            if (force) {
                existing.get().enable();
            }
            return;
        }
        assignmentRepository.save(TeacherAssistantAssignment.create(teacherId, assistantId));
    }

    private List<Member> loadMembers(List<String> emails, MemberRole role) {
        return emails.stream()
                .map(email -> memberRepository.findByEmail(email)
                        .filter(member -> member.getRole() == role)
                        .orElseGet(() -> {
                            log.warn("Member not found for email={}", email);
                            return null;
                        }))
                .filter(member -> member != null)
                .toList();
    }
}
